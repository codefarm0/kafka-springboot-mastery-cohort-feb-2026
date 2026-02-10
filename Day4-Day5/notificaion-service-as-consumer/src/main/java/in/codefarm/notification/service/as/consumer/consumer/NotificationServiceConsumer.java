package in.codefarm.notification.service.as.consumer.consumer;

import in.codefarm.notification.service.as.consumer.event.OrderPlacedEvent;
import in.codefarm.notification.service.as.consumer.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.Executors;

@Component
public class NotificationServiceConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceConsumer.class);
    private static final String CONSUMER_GROUP = "notification-service-group";
    
    private final NotificationService notificationService;

    private final ObjectMapper objectMapper;
    
    public NotificationServiceConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "orders",
        groupId = CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAutoCommit(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) throws InterruptedException {
       log.info("event received {}",
           record.value());

        OrderPlacedEvent event = objectMapper.readValue(record.value(), OrderPlacedEvent.class);
        try {
            // Send notification
            notificationService.sendEmailNotification(event);

            Thread.sleep(100);//processing
            // Save to database
            notificationService.saveNotification(
                event,
                "EMAIL",
                "SENT",
                "Order confirmation email sent",
                    record.partition(),
                    record.offset(),
                CONSUMER_GROUP,
                "auto-commit"
            );
            
            log.info("Notification processed successfully for order {} ===",
                event.orderId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order {} ===", event.orderId(), e);
            throw e;
        }
    }
}

