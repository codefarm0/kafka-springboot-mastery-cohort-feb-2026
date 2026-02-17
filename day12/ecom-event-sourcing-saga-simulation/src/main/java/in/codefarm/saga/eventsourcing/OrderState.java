package in.codefarm.saga.eventsourcing;

import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.OrderPlacedEvent;
import in.codefarm.saga.event.PaymentProcessedEvent;
import in.codefarm.saga.event.InventoryReservedEvent;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Order State - Reconstructed from events.
 * This class applies events in sequence to build the current state of an order.
 */
public class OrderState {
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;  // PLACED, PAYMENT_COMPLETED, INVENTORY_RESERVED, COMPLETED, CANCELLED, etc.
    private String paymentId;
    private String paymentStatus;
    private String inventoryReservationId;
    private String inventoryStatus;
    private List<String> eventHistory;
    private transient JsonMapper jsonMapper;  // Transient - not serialized in snapshots
    
    // Default constructor for JSON deserialization
    public OrderState() {
        this.eventHistory = new ArrayList<>();
    }
    
    public OrderState(String orderId, JsonMapper jsonMapper) {
        this.orderId = orderId;
        this.status = "UNKNOWN";
        this.eventHistory = new ArrayList<>();
        this.jsonMapper = jsonMapper;
    }
    
    // Setter for jsonMapper (used after deserialization from snapshot)
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }
    
    // Apply events to build state
    public void applyOrderPlaced(EventWrapper<?> event) {
        OrderPlacedEvent payload = jsonMapper.convertValue(event.payload(), OrderPlacedEvent.class);
        this.customerId = payload.customerId();
        this.productId = payload.productId();
        this.quantity = payload.quantity();
        this.totalAmount = payload.totalAmount();
        this.status = "PLACED";
        this.eventHistory.add(event.metadata().eventType());
    }
    
    public void applyPaymentProcessed(EventWrapper<?> event) {
        PaymentProcessedEvent payload = jsonMapper.convertValue(event.payload(), PaymentProcessedEvent.class);
        this.paymentId = payload.paymentId();
        this.paymentStatus = payload.status();
        if ("SUCCESS".equals(payload.status())) {
            this.status = "PAYMENT_COMPLETED";
        } else {
            this.status = "PAYMENT_FAILED";
        }
        this.eventHistory.add(event.metadata().eventType());
    }
    
    public void applyInventoryReserved(EventWrapper<?> event) {
        InventoryReservedEvent payload = jsonMapper.convertValue(event.payload(), InventoryReservedEvent.class);
        this.inventoryReservationId = payload.reservationId();
        this.inventoryStatus = payload.status();
        if ("RESERVED".equals(payload.status())) {
            this.status = "INVENTORY_RESERVED";
        }
        this.eventHistory.add(event.metadata().eventType());
    }
    
    public void applyOrderCancelled(EventWrapper<?> event) {
        // Order is cancelled - final state
        // If payment was refunded, paymentStatus should already be REFUNDED
        // If payment wasn't refunded but order is cancelled, paymentStatus remains as is
        this.status = "CANCELLED";
        this.eventHistory.add(event.metadata().eventType());
    }
    
    public void applyPaymentRefunded(EventWrapper<?> event) {
        // Update payment status to reflect refund
        this.paymentStatus = "REFUNDED";
        this.status = "PAYMENT_REFUNDED";
        this.eventHistory.add(event.metadata().eventType());
    }
    
    // Getters and Setters (for JSON serialization/deserialization)
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public String getInventoryReservationId() { return inventoryReservationId; }
    public void setInventoryReservationId(String inventoryReservationId) { this.inventoryReservationId = inventoryReservationId; }
    
    public String getInventoryStatus() { return inventoryStatus; }
    public void setInventoryStatus(String inventoryStatus) { this.inventoryStatus = inventoryStatus; }
    
    public List<String> getEventHistory() { return eventHistory; }
    public void setEventHistory(List<String> eventHistory) { this.eventHistory = eventHistory; }
}

