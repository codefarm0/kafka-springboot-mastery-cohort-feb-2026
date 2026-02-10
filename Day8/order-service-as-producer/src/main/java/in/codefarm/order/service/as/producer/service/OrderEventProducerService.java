package in.codefarm.order.service.as.producer.service;

import in.codefarm.order.service.as.producer.entity.OrderEntity;
import in.codefarm.order.service.as.producer.event.OrderPlacedEvent;
import in.codefarm.order.service.as.producer.event.OrderRequest;
import in.codefarm.order.service.as.producer.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderEventProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventProducerService.class);
    private static final String TOPIC_NAME = "orders";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    
    public OrderEventProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            OrderRepository orderRepository, ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }
    
    // Scenario 1: Fire-and-Forget (Async, No Wait)
    @Transactional
    public OrderEntity createOrder(OrderRequest request) {
        log.info("===  creating order {} ===", request.customerId());
        var event = createEvent(request);

        
        // Send and don't wait - fire and forget
        kafkaTemplate.send(TOPIC_NAME, event.orderId(), objectMapper.writeValueAsString(event));
        log.info("===  Message sent for order {} ===", event.orderId());

        // Save to database
        var orderEntity = saveOrderToDatabase(event);
        return orderEntity;
    }


    // Helper method to create event from request
    private OrderPlacedEvent createEvent(OrderRequest request) {
        return new OrderPlacedEvent(
                UUID.randomUUID().toString(),
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.totalAmount(),
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );
    }
    
    // Helper method to save order to database
    private OrderEntity saveOrderToDatabase(OrderPlacedEvent event) {
        log.debug("Saving order {} to database", event.orderId());
        
        var orderEntity = new OrderEntity(
            event.orderId(),
            event.customerId(),
            event.productId(),
            event.quantity(),
            event.totalAmount(),
            event.orderDate()
        );
        
        var saved = orderRepository.save(orderEntity);
        log.info("Order {} saved to database with ID: {}", event.orderId(), saved.getId());
        
        return saved;
    }
}

