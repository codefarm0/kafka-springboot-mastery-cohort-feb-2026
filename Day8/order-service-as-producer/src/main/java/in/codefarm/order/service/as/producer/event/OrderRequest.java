package in.codefarm.order.service.as.producer.event;

import java.math.BigDecimal;

// Request DTO
    public record OrderRequest(
        String customerId,
        String productId,
        Integer quantity,
        BigDecimal totalAmount
    ) {
    }