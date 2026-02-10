package in.codefarm.order.service.as.producer.controller;

import in.codefarm.order.service.as.producer.event.OrderPlacedEvent;
import in.codefarm.order.service.as.producer.event.OrderRequest;
import in.codefarm.order.service.as.producer.service.OrderEventProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderEventProducerService orderEventProducerService;
    
    public OrderController(OrderEventProducerService orderEventProducerService) {
        this.orderEventProducerService = orderEventProducerService;
    }
    


    
    // Scenario 1: Fire-and-Forget
    @PostMapping("")
    public ResponseEntity<String> createOrder(@RequestBody OrderRequest request) {
        log.info("=== REST Endpoint: POST /api/orders ===");

            var orderEntity = orderEventProducerService.createOrder(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed, Order ID: " + orderEntity.getOrderId() +
                      ", DB ID: " + orderEntity.getId());

    }

}

