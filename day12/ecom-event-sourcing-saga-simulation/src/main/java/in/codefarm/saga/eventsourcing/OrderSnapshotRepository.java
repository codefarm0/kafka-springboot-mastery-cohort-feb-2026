package in.codefarm.saga.eventsourcing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Order Snapshot entities.
 */
@Repository
public interface OrderSnapshotRepository extends JpaRepository<OrderSnapshot, Long> {
    
    /**
     * Find the latest snapshot for a specific order.
     */
    Optional<OrderSnapshot> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);
}

