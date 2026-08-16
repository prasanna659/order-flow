package com.orderflow.inventory.repository;

import com.orderflow.inventory.entity.Reservation;
import com.orderflow.inventory.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);
}
