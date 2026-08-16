package com.orderflow.order.repository;

import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND (:status IS NULL OR o.status = :status) ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdWithOptionalStatus(@Param("userId") Long userId, @Param("status") OrderStatus status, Pageable pageable);
    
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
