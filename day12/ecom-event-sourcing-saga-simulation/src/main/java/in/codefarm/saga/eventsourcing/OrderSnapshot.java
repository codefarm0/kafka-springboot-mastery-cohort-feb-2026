package in.codefarm.saga.eventsourcing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Order Snapshot Entity - Stores periodic snapshots of order state for faster replay.
 * Snapshots are created periodically (e.g., every 100 events) to optimize replay performance.
 */
@Entity
@Table(name = "order_snapshots", indexes = {
    @Index(name = "idx_order_snapshot_order_id", columnList = "orderId"),
    @Index(name = "idx_order_snapshot_created", columnList = "createdAt")
})
public class OrderSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String orderId;
    
    @Column(nullable = false)
    private Long eventOffset;  // Last event offset included in snapshot
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String stateJson;   // Serialized OrderState as JSON
    
    @Column(nullable = false)
    private Integer eventCount;  // Number of events processed up to this snapshot
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    public OrderSnapshot() {
    }
    
    public OrderSnapshot(String orderId, Long eventOffset, String stateJson, Integer eventCount) {
        this.orderId = orderId;
        this.eventOffset = eventOffset;
        this.stateJson = stateJson;
        this.eventCount = eventCount;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public Long getEventOffset() { return eventOffset; }
    public void setEventOffset(Long eventOffset) { this.eventOffset = eventOffset; }
    
    public String getStateJson() { return stateJson; }
    public void setStateJson(String stateJson) { this.stateJson = stateJson; }
    
    public Integer getEventCount() { return eventCount; }
    public void setEventCount(Integer eventCount) { this.eventCount = eventCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

