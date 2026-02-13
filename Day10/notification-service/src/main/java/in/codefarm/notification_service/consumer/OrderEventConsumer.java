package in.codefarm.notification_service.consumer;

import in.codefarm.notification_service.model.Notification;
import in.codefarm.notification_service.model.OrderEvent;
import in.codefarm.notification_service.model.OutboxEvent;
import in.codefarm.notification_service.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Component
@Slf4j
public class OrderEventConsumer {


    private final NotificationRepository notificationRepository;
    private final ObjectMapper mapper;

    public OrderEventConsumer(NotificationRepository notificationRepository, ObjectMapper mapper) {
        this.notificationRepository = notificationRepository;
        this.mapper = mapper;
    }

    @RetryableTopic(
            attempts = "3",  // Retry 3 times
            backOff = @BackOff(delay = 1000, multiplier = 2.0),  // 1s, 2s, 4s delays
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {RuntimeException.class}  // Retry on runtime exceptions
    )
    @KafkaListener(
            topics = "order_events",
            groupId = "notification-service-group",
            concurrency = "3"
    )
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        OrderEvent event = mapper.readValue(record.value(), OrderEvent.class);

        Notification notification = Notification.builder()
                .orderId(event.getId())
                .userId(event.getUserId())
                .courseId(event.getCourseId())
                .status("SENT")
                .processedAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        sendNotication(event.getCourseId(), event.getUserId());
    }

    private void sendNotication(Long courseId, Long userId) {
        // Simulate an unreliable downstream system ~30% of the time
/*        if (Math.random() < 0.3) {
            log.warn("Downstream system is slow/unavailable for course {}", userId);
            throw new RuntimeException("Downstream timeout");
        }
//         Simulate some processing time
        try {
            Thread.sleep(500); // 0.5s
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }*/
        log.info("sent notification for user {}", userId);
    }

        @DltHandler
    public void handleDlt(
            String payload,
            @Header(KafkaHeaders.ORIGINAL_PARTITION) int partition,
            @Header(KafkaHeaders.ORIGINAL_TIMESTAMP) long timestamp,
            @Header(KafkaHeaders.ORIGINAL_TOPIC) String topic,
            @Header("retry_topic-attempts") int attempts,
         @Header("retry_topic-backoff-timestamp") String backOfftimestamp) {

        log.error("""
                        Moved to DLT
                        Topic      : {}
                        Partition  : {}
                        Timestamp  : {}
                        Attempts   : {}
                        Payload    : {}
                         backOfftimestamp    : {}
                        """,
                topic,
                partition,
                Instant.ofEpochMilli(timestamp),
                attempts,
                payload,
                backOfftimestamp
        );
    }
}
