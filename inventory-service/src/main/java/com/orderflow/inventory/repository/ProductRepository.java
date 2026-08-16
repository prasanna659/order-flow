package com.orderflow.inventory.repository;

import com.orderflow.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // PESSIMISTIC_WRITE locks the row for the duration of the transaction --
    // this prevents two concurrent orders from both reading "5 in stock"
    // and both successfully reserving the last item. It's the simplest fix
    // for the classic check-then-act race condition in stock reservation.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findById(Long id);
}
