package in.codefarm.saga.eventsourcing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Event Replay operations.
 * Provides endpoints to replay events and reconstruct order state.
 */
@RestController
@RequestMapping("/api/replay")
public class ReplayController {
    
    private static final Logger log = LoggerFactory.getLogger(ReplayController.class);
    
    private final OrderReplayService replayService;
    
    public ReplayController(OrderReplayService replayService) {
        this.replayService = replayService;
    }
    
    /**
     * Replay all events for a specific order and return reconstructed state.
     * 
     * GET /api/replay/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> replayOrder(@PathVariable String orderId) {
        log.info("Replay request received for order: {}", orderId);
        
        try {
            OrderState state = replayService.replayOrder(orderId);
            OrderReplayResponse response = OrderReplayResponse.from(state);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error replaying order: {}", orderId, e);
            ErrorResponse error = new ErrorResponse("Failed to replay order: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Replay events up to a specific timestamp and return state at that point.
     * 
     * GET /api/replay/order/{orderId}/at?timestamp=2024-01-15T10:30:00
     */
    @GetMapping("/order/{orderId}/at")
    public ResponseEntity<?> replayOrderAtTimestamp(
        @PathVariable String orderId,
        @RequestParam String timestamp
    ) {
        log.info("Replay request received for order: {} at timestamp: {}", orderId, timestamp);
        
        try {
            LocalDateTime targetTime = LocalDateTime.parse(timestamp);
            OrderState state = replayService.replayToTimestamp(orderId, targetTime);
            OrderReplayAtTimestampResponse response = OrderReplayAtTimestampResponse.from(state, timestamp);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error replaying order at timestamp: {}", orderId, e);
            ErrorResponse error = new ErrorResponse("Failed to replay order: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}

