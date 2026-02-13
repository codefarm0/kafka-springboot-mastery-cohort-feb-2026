package in.codefarm.notification_service.model;


import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String type;
    private String payload;
    private String status; // e.g. NEW, SENT
    private Instant createdAt;
    private Instant sentAt;
    // getters/setters
}