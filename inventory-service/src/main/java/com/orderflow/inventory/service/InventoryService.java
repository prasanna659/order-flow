package com.orderflow.inventory.service;

import com.orderflow.inventory.dto.ReleaseRequest;
import com.orderflow.inventory.dto.ReserveItem;
import com.orderflow.inventory.dto.ReserveRequest;
import com.orderflow.inventory.entity.Product;
import com.orderflow.inventory.entity.Reservation;
import com.orderflow.inventory.entity.ReservationStatus;
import com.orderflow.inventory.exception.InsufficientStockException;
import com.orderflow.inventory.exception.ResourceNotFoundException;
import com.orderflow.inventory.repository.ProductRepository;
import com.orderflow.inventory.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    public InventoryService(ProductRepository productRepository, ReservationRepository reservationRepository) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    // All-or-nothing: if ANY item in the order can't be fully reserved, the
    // whole @Transactional method throws and Spring rolls back every
    // decrement that already happened inside it. This is a LOCAL ACID
    // transaction, scoped to this one service's database only -- it does
    // NOT span into order-service or payment-service. That's the boundary
    // the saga pattern exists to work around.
    @Transactional
    public void reserve(ReserveRequest request) {
        for (ReserveItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + item.getProductId()));

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "INSUFFICIENT_STOCK",
                        "Insufficient stock for product " + product.getName()
                                + " (requested " + item.getQuantity() + ", available " + product.getStockQuantity() + ")");
            }
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);

            reservationRepository.save(new Reservation(
                    request.getOrderId(), item.getProductId(), item.getQuantity(), ReservationStatus.RESERVED));
        }
    }

    // The compensating transaction. Called by order-service ONLY when a
    // later saga step (payment) fails after this step already succeeded.
    // It restores exactly what was decremented, using the Reservation
    // records as the source of truth -- never re-derives quantities from
    // the request, since the request that triggered release() might not
    // even carry item details.
    @Transactional
    public void release(ReleaseRequest request) {
        List<Reservation> reservations =
                reservationRepository.findByOrderIdAndStatus(request.getOrderId(), ReservationStatus.RESERVED);

        for (Reservation reservation : reservations) {
            Product product = productRepository.findById(reservation.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + reservation.getProductId()));
            product.setStockQuantity(product.getStockQuantity() + reservation.getQuantity());
            productRepository.save(product);
            reservation.setStatus(ReservationStatus.RELEASED);
        }
    }
}
