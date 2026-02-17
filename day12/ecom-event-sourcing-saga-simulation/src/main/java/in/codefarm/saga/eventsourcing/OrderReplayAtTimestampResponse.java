package in.codefarm.saga.eventsourcing;

import java.util.List;

/**
 * Response DTO for order replay at timestamp operations.
 */
public record OrderReplayAtTimestampResponse(
    String orderId,
    String status,
    List<String> eventHistory,
    String replayedUpTo
) {
    public static OrderReplayAtTimestampResponse from(OrderState state, String timestamp) {
        return new OrderReplayAtTimestampResponse(
            state.getOrderId(),
            state.getStatus(),
            state.getEventHistory(),
            timestamp
        );
    }
}

