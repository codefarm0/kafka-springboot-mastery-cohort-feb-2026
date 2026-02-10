package in.codefarm.order.service.as.producer.repository;

import in.codefarm.order.service.as.producer.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}

