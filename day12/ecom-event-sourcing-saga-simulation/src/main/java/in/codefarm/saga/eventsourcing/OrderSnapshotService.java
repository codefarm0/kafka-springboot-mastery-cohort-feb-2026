package in.codefarm.saga.eventsourcing;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * Order Snapshot Service - Creates periodic snapshots of order state for faster replay.
 * 
 * Snapshot Strategy:
 * 1. Time-based: Create snapshots for orders older than X days (scheduled task)
 * 2. On-demand: Create snapshot after replay if it doesn't exist
 * 
 * Note: Event-count-based snapshots don't work well since each order has limited events (5-6).
 */
@Service
public class OrderSnapshotService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderSnapshotService.class);
    private static final String EVENT_STORE_TOPIC = "order-events";
    private static final int SNAPSHOT_AGE_DAYS = 1;  // Create snapshot for orders older than 1 day
    
    private final KafkaConsumer<String, in.codefarm.saga.event.EventWrapper<?>> snapshotConsumer;
    private final OrderSnapshotRepository snapshotRepository;
    private final JsonMapper jsonMapper;
    private final in.codefarm.saga.order.repository.OrderRepository orderRepository;
    
    public OrderSnapshotService(
        @Qualifier("replayKafkaConsumer") KafkaConsumer<String, in.codefarm.saga.event.EventWrapper<?>> snapshotConsumer,
        OrderSnapshotRepository snapshotRepository,
        JsonMapper jsonMapper,
        in.codefarm.saga.order.repository.OrderRepository orderRepository
    ) {
        this.snapshotConsumer = snapshotConsumer;
        this.snapshotRepository = snapshotRepository;
        this.jsonMapper = jsonMapper;
        this.orderRepository = orderRepository;
    }
    
    /**
     * Create snapshot for an order if it doesn't exist.
     * Called on-demand after replay operations.
     * 
     * @param orderId The order ID
     * @param eventOffset The last event offset processed
     */
    public void createSnapshotIfNeeded(String orderId, long eventOffset) {
        // Check if snapshot already exists for this order
        var existingSnapshot = snapshotRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        if (existingSnapshot.isPresent()) {
            log.debug("Snapshot already exists for order: {}, skipping", orderId);
            return;
        }
        
        // Create snapshot on-demand
        log.info("Creating on-demand snapshot for order: {} at offset: {}", orderId, eventOffset);
        createSnapshot(orderId, eventOffset);
    }
    
    /**
     * Create a snapshot for a specific order.
     */
    private void createSnapshot(String orderId, long eventOffset) {
        try {
            // Reconstruct state up to this point
            OrderState state = reconstructStateUpToOffset(orderId, eventOffset);
            
            // Serialize state to JSON
            String stateJson = jsonMapper.writeValueAsString(state);
            
            // Count events for this order
            int eventCount = state.getEventHistory().size();
            
            // Save snapshot
            OrderSnapshot snapshot = new OrderSnapshot(orderId, eventOffset, stateJson, eventCount);
            snapshotRepository.save(snapshot);
            
            log.info("Snapshot created for order: {} at offset: {}, event count: {}", 
                orderId, eventOffset, eventCount);
            
        } catch (Exception e) {
            log.error("Failed to create snapshot for order: {}", orderId, e);
        }
    }
    
    /**
     * Reconstruct state up to a specific offset.
     */
    private OrderState reconstructStateUpToOffset(String orderId, long targetOffset) {
        OrderState state = new OrderState(orderId, jsonMapper);
        
        synchronized (snapshotConsumer) {
            var partitions = snapshotConsumer.partitionsFor(EVENT_STORE_TOPIC);
            if (partitions == null || partitions.isEmpty()) {
                return state;
            }
            
            var topicPartitions = partitions.stream()
                .map(p -> new TopicPartition(EVENT_STORE_TOPIC, p.partition()))
                .toList();
            snapshotConsumer.assign(topicPartitions);
            snapshotConsumer.seekToBeginning(topicPartitions);
            
            while (true) {
                ConsumerRecords<String, in.codefarm.saga.event.EventWrapper<?>> records = 
                    snapshotConsumer.poll(Duration.ofSeconds(1));
                if (records.isEmpty()) {
                    break;
                }
                
                for (ConsumerRecord<String, in.codefarm.saga.event.EventWrapper<?>> record : records) {
                    if (record.key().equals(orderId)) {
                        if (record.offset() > targetOffset) {
                            return state; // Stop at target offset
                        }
                        applyEventToState(record.value(), state);
                    }
                }
            }
        }
        
        return state;
    }
    
    private void applyEventToState(in.codefarm.saga.event.EventWrapper<?> event, OrderState state) {
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
        }
    }
    
    /**
     * Scheduled task to create snapshots for orders older than SNAPSHOT_AGE_DAYS.
     * Runs daily at 2 AM.
     * 
     * Strategy: Create snapshots for orders that are old enough to benefit from snapshots,
     * but don't have one yet. This ensures older orders have snapshots for faster replay.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void createSnapshotsForOldOrders() {
        log.info("Running scheduled snapshot creation for old orders");
        
        try {
            // Get all orders from database
            var orders = orderRepository.findAll();
            int snapshotsCreated = 0;
            
            for (var order : orders) {
                String orderId = order.getOrderId();
                
                // Check if order is old enough (older than SNAPSHOT_AGE_DAYS)
                if (order.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusDays(SNAPSHOT_AGE_DAYS))) {
                    // Check if snapshot already exists
                    var existingSnapshot = snapshotRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
                    if (existingSnapshot.isEmpty()) {
                        // Find the last event offset for this order
                        long lastOffset = findLastEventOffset(orderId);
                        if (lastOffset >= 0) {
                            createSnapshot(orderId, lastOffset);
                            snapshotsCreated++;
                        }
                    }
                }
            }
            
            log.info("Scheduled snapshot creation completed. Created {} snapshots", snapshotsCreated);
            
        } catch (Exception e) {
            log.error("Error during scheduled snapshot creation", e);
        }
    }
    
    /**
     * Find the last event offset for an order.
     * Returns -1 if no events found.
     */
    private long findLastEventOffset(String orderId) {
        synchronized (snapshotConsumer) {
            var partitions = snapshotConsumer.partitionsFor(EVENT_STORE_TOPIC);
            if (partitions == null || partitions.isEmpty()) {
                return -1;
            }
            
            var topicPartitions = partitions.stream()
                .map(p -> new TopicPartition(EVENT_STORE_TOPIC, p.partition()))
                .toList();
            snapshotConsumer.assign(topicPartitions);
            snapshotConsumer.seekToBeginning(topicPartitions);
            
            long lastOffset = -1;
            while (true) {
                ConsumerRecords<String, in.codefarm.saga.event.EventWrapper<?>> records = 
                    snapshotConsumer.poll(Duration.ofSeconds(1));
                if (records.isEmpty()) {
                    break;
                }
                
                for (ConsumerRecord<String, in.codefarm.saga.event.EventWrapper<?>> record : records) {
                    if (record.key().equals(orderId)) {
                        lastOffset = record.offset();
                    }
                }
            }
            
            return lastOffset;
        }
    }
}

