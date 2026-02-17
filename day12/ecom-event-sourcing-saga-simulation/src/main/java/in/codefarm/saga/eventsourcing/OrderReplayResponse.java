package in.codefarm.saga.eventsourcing;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for order replay operations.
 */
public record OrderReplayResponse(
    String orderId,
    String customerId,
    String productId,
    Integer quantity,
    BigDecimal totalAmount,
    String status,
    String paymentId,
    String paymentStatus,
    String inventoryReservationId,
    String inventoryStatus,
    List<String> eventHistory
) {
    public static OrderReplayResponse from(OrderState state) {
        return new OrderReplayResponse(
            state.getOrderId(),
            state.getCustomerId(),
            state.getProductId(),
            state.getQuantity(),
            state.getTotalAmount(),
            state.getStatus(),
            state.getPaymentId(),
            state.getPaymentStatus(),
            state.getInventoryReservationId(),
            state.getInventoryStatus(),
            state.getEventHistory()
        );
    }
}

