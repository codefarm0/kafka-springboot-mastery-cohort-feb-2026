package in.codefarm.notification.service.as.consumer.service;

import in.codefarm.notification.service.as.consumer.entity.NotificationEntity;
import in.codefarm.notification.service.as.consumer.event.OrderPlacedEvent;
import in.codefarm.notification.service.as.consumer.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationRepository notificationRepository;
    
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }
    
    @Transactional
    public NotificationEntity saveNotification(
        OrderPlacedEvent event,
        String notificationType,
        String status,
        String message,
        Integer partition,
        Long offset,
        String consumerGroup,
        String consumerMethod
    ) {
        log.debug("Saving notification for order {} to database", event.orderId());
        
        var notification = new NotificationEntity(
            event.orderId(),
            event.customerId(),
            notificationType,
            status,
            message,
            partition,
            offset,
            consumerGroup,
            consumerMethod
        );

        Optional<NotificationEntity> byOrderId = notificationRepository.findByOrderId(event.orderId());
        if(byOrderId.isPresent()){
            log.info("this order was already processed - {}", event.orderId());
            return null;
        }
        var saved = notificationRepository.save(notification);
        log.info("Notification saved to database - ID: {}, Order ID: {}", saved.getId(), event.orderId());
        
        return saved;
    }
    
    public void sendEmailNotification(OrderPlacedEvent event)  {
        log.info("Sending email notification to customer: {}", event.customerId());
        log.info("Email: Your order {} for ${} has been placed successfully!", 
            event.orderId(), 
            event.totalAmount());
//        sendNotication(event.orderId(), event.customerId());
        // In real system: call email service
        // emailService.sendConfirmationEmail(event.customerId(), event.orderId(), event.totalAmount());
    }
}

