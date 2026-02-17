package in.codefarm.saga.eventsourcing;

import in.codefarm.saga.event.EventWrapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Order Replay Service - Replays events from event store to reconstruct order state.
 * Supports full replay, partial replay by orderId, and time-based replay.
 */
@Service
public class OrderReplayService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderReplayService.class);
    private static final String EVENT_STORE_TOPIC = "order-events";
    
    private final KafkaConsumer<String, EventWrapper<?>> replayConsumer;
    private final JsonMapper jsonMapper;
    private final OrderSnapshotRepository snapshotRepository;
    private final OrderSnapshotService snapshotService;
    
    public OrderReplayService(
        @Qualifier("replayKafkaConsumer") KafkaConsumer<String, EventWrapper<?>> replayConsumer,
        JsonMapper jsonMapper,
        OrderSnapshotRepository snapshotRepository,
        OrderSnapshotService snapshotService
    ) {
        this.replayConsumer = replayConsumer;
        this.jsonMapper = jsonMapper;
        this.snapshotRepository = snapshotRepository;
        this.snapshotService = snapshotService;
    }
    
    /**
     * Reconstruct order state by replaying all events for a specific order.
     * Uses snapshot if available for faster replay.
     * 
     * @param orderId The order ID to replay events for
     * @return Reconstructed order state
     */
    public OrderState replayOrder(String orderId) {
        log.info("Replaying events for order: {}", orderId);
        
        // Check for snapshot
        var snapshotOpt = snapshotRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        
        if (snapshotOpt.isPresent()) {
            OrderSnapshot snapshot = snapshotOpt.get();
            log.info("Found snapshot for order: {} at offset: {}, event count: {}", 
                orderId, snapshot.getEventOffset(), snapshot.getEventCount());
            
            // Start from snapshot state
            try {
                OrderState state = jsonMapper.readValue(snapshot.getStateJson(), OrderState.class);
                // Restore jsonMapper after deserialization (it's transient)
                state.setJsonMapper(jsonMapper);
                
                // Replay events after snapshot
                replayEventsAfterOffset(orderId, snapshot.getEventOffset(), state);
                
                log.info("Replayed from snapshot + remaining events for order: {}", orderId);
                return state;
            } catch (Exception e) {
                log.warn("Failed to deserialize snapshot for order: {}, falling back to full replay", orderId, e);
                // Fall through to full replay
            }
        }
        
        // No snapshot or snapshot deserialization failed - full replay
        return replayAllEvents(orderId);
    }
    
    /**
     * Replay all events from beginning (no snapshot).
     */
    private OrderState replayAllEvents(String orderId) {
        OrderState state = new OrderState(orderId, jsonMapper);
        
        synchronized (replayConsumer) {
            var partitions = replayConsumer.partitionsFor(EVENT_STORE_TOPIC);
            if (partitions == null || partitions.isEmpty()) {
                log.warn("No partitions found for topic: {}", EVENT_STORE_TOPIC);
                return state;
            }
            
            var topicPartitions = partitions.stream()
                .map(p -> new TopicPartition(EVENT_STORE_TOPIC, p.partition()))
                .toList();
            replayConsumer.assign(topicPartitions);
            replayConsumer.seekToBeginning(topicPartitions);
            
            int eventsProcessed = 0;
            long lastOffset = -1;
            while (true) {
                ConsumerRecords<String, EventWrapper<?>> records = replayConsumer.poll(Duration.ofSeconds(1));
                if (records.isEmpty()) {
                    break;
                }
                
                for (ConsumerRecord<String, EventWrapper<?>> record : records) {
                    if (record.key().equals(orderId)) {
                        applyEventToState(record.value(), state);
                        eventsProcessed++;
                        lastOffset = record.offset();
                    }
                }
            }
            
            // Create snapshot on-demand if it doesn't exist (for future faster replays)
            if (lastOffset >= 0) {
                snapshotService.createSnapshotIfNeeded(orderId, lastOffset);
            }
            
            log.info("Replayed {} events for order: {} (full replay)", eventsProcessed, orderId);
        }
        
        return state;
    }
    
    /**
     * Replay events after a specific offset (for snapshot-based replay).
     */
    private void replayEventsAfterOffset(String orderId, long snapshotOffset, OrderState state) {
        synchronized (replayConsumer) {
            var partitions = replayConsumer.partitionsFor(EVENT_STORE_TOPIC);
            if (partitions == null || partitions.isEmpty()) {
                return;
            }
            
            var topicPartitions = partitions.stream()
                .map(p -> new TopicPartition(EVENT_STORE_TOPIC, p.partition()))
                .toList();
            replayConsumer.assign(topicPartitions);
            replayConsumer.seekToBeginning(topicPartitions);
            
            int eventsProcessed = 0;
            while (true) {
                ConsumerRecords<String, EventWrapper<?>> records = replayConsumer.poll(Duration.ofSeconds(1));
                if (records.isEmpty()) {
                    break;
                }
                
                for (ConsumerRecord<String, EventWrapper<?>> record : records) {
                    if (record.key().equals(orderId) && record.offset() > snapshotOffset) {
                        applyEventToState(record.value(), state);
                        eventsProcessed++;
                    }
                }
            }
            
            log.info("Replayed {} events after snapshot offset {} for order: {}", 
                eventsProcessed, snapshotOffset, orderId);
        }
    }
    
    /**
     * Replay events up to a specific timestamp.
     * 
     * @param orderId The order ID
     * @param timestamp The timestamp to replay up to
     * @return Order state at that point in time
     */
    public OrderState replayToTimestamp(String orderId, LocalDateTime timestamp) {
        log.info("Replaying events for order: {} up to timestamp: {}", orderId, timestamp);
        
        OrderState state = new OrderState(orderId, jsonMapper);
        
        synchronized (replayConsumer) {
            var partitions = replayConsumer.partitionsFor(EVENT_STORE_TOPIC);
            if (partitions == null || partitions.isEmpty()) {
                return state;
            }
            
            var topicPartitions = partitions.stream()
                .map(p -> new TopicPartition(EVENT_STORE_TOPIC, p.partition()))
                .toList();
            replayConsumer.assign(topicPartitions);
            replayConsumer.seekToBeginning(topicPartitions);
            
            while (true) {
                ConsumerRecords<String, EventWrapper<?>> records = replayConsumer.poll(Duration.ofSeconds(1));
                if (records.isEmpty()) {
                    break;
                }
                
                for (ConsumerRecord<String, EventWrapper<?>> record : records) {
                    if (record.key().equals(orderId)) {
                        // Check if event happened before or at target time
                        LocalDateTime eventTime = record.value().metadata().timestamp();
                        if (eventTime.isAfter(timestamp)) {
                            return state; // Stop replaying
                        }
                        
                        applyEventToState(record.value(), state);
                    }
                }
            }
        }
        
        return state;
    }
    
    private void applyEventToState(EventWrapper<?> event, OrderState state) {
        String eventType = event.metadata().eventType();
        
        switch (eventType) {
            case "OrderPlaced":
                state.applyOrderPlaced(event);
                break;
            case "PaymentProcessed":
                state.applyPaymentProcessed(event);
                break;
            case "InventoryReserved":
                state.applyInventoryReserved(event);
                break;
            case "OrderCancelled":
                state.applyOrderCancelled(event);
                break;
            case "PaymentRefunded":
                state.applyPaymentRefunded(event);
                break;
            default:
                log.debug("Unknown event type: {}", eventType);
        }
    }
}

