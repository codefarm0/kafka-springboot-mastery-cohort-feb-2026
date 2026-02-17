package in.codefarm.saga.inventory.service;

import in.codefarm.saga.event.EventMetadata;
import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.InventoryReservedEvent;
import in.codefarm.saga.eventsourcing.EventStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class InventoryEventProducer {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryEventProducer.class);
    private static final String TOPIC_NAME = "inventory";
    
    private final KafkaTemplate<String, EventWrapper<?>> kafkaTemplate;
    private final EventStoreService eventStoreService;
    
    public InventoryEventProducer(
        KafkaTemplate<String, EventWrapper<?>> kafkaTemplate,
        EventStoreService eventStoreService
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventStoreService = eventStoreService;
    }
    
    public void sendInventoryReservedEvent(InventoryReservedEvent payload, String transactionId) {
        var metadata = new EventMetadata(
            "InventoryReserved",
            "1.0",
            "inventory-service",
            transactionId,
            LocalDateTime.now()
        );
        
        var event = new EventWrapper<>(metadata, payload);
        
        log.info("Publishing InventoryReservedEvent - OrderId: {}, Status: {}, TransactionId: {}", 
            payload.orderId(), payload.status(), transactionId);
        
        CompletableFuture<SendResult<String, EventWrapper<?>>> future = 
            kafkaTemplate.send(TOPIC_NAME, payload.orderId(), event);
        
        future.thenAccept(result -> {
            var recordMetadata = result.getRecordMetadata();
            log.info("Inventory event sent - OrderId: {}, Partition: {}, Offset: {}", 
                payload.orderId(), recordMetadata.partition(), recordMetadata.offset());
        });
        
        future.exceptionally(ex -> {
            log.error("Failed to send inventory event: {}", ex.getMessage(), ex);
            return null;
        });
        
        // Also append to event store for event sourcing
        eventStoreService.appendEvent(payload.orderId(), event);
    }
}

