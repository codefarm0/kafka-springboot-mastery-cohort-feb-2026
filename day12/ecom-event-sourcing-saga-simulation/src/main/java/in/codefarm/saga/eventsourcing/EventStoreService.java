package in.codefarm.saga.eventsourcing;

import in.codefarm.saga.event.EventWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Event Store Service - Publishes all events to order-events topic for event sourcing.
 * This topic serves as the event store where all events are persisted for replay.
 */
@Service
public class EventStoreService {
    
    private static final Logger log = LoggerFactory.getLogger(EventStoreService.class);
    private static final String EVENT_STORE_TOPIC = "order-events";
    
    private final KafkaTemplate<String, EventWrapper<?>> kafkaTemplate;
    
    public EventStoreService(KafkaTemplate<String, EventWrapper<?>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Append event to event store (order-events topic).
     * Key: orderId (ensures all events for same order are in same partition)
     * Value: EventWrapper (contains event metadata and payload)
     * 
     * @param orderId The order ID (used as Kafka key for partitioning)
     * @param event The event wrapper containing metadata and payload
     */
    public void appendEvent(String orderId, EventWrapper<?> event) {
        log.debug("Appending event to event store - OrderId: {}, EventType: {}", 
            orderId, event.metadata().eventType());
        
        CompletableFuture<SendResult<String, EventWrapper<?>>> future = 
            kafkaTemplate.send(EVENT_STORE_TOPIC, orderId, event);
        
        future.thenAccept(result -> {
            var recordMetadata = result.getRecordMetadata();
            log.debug("Event appended to event store - OrderId: {}, EventType: {}, Partition: {}, Offset: {}", 
                orderId, 
                event.metadata().eventType(),
                recordMetadata.partition(), 
                recordMetadata.offset());
        });
        
        future.exceptionally(ex -> {
            log.error("Failed to append event to event store - OrderId: {}, EventType: {}", 
                orderId, event.metadata().eventType(), ex);
            return null;
        });
    }
}

