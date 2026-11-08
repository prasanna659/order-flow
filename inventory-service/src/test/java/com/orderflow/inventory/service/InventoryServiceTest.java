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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ReservationRepository reservationRepository;
    @InjectMocks private InventoryService inventoryService;

    private Product product(int stock) {
        return new Product("Keyboard", "desc", new BigDecimal("99.99"), stock, null);
    }

    private ReserveRequest request(String orderId, long productId, int qty) {
        ReserveRequest r = new ReserveRequest();
        r.setOrderId(orderId);
        r.setItems(List.of(new ReserveItem(productId, qty)));
        return r;
    }

    @Test
    @DisplayName("reserve decrements stock and records a RESERVED reservation")
    void reserveDecrementsStock() {
        Product p = product(5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        inventoryService.reserve(request("o-1", 1L, 2));

        assertEquals(3, p.getStockQuantity());
        verify(productRepository).save(p);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("reserve rejects an order that asks for more than is in stock, and changes nothing")
    void reserveRejectsInsufficientStock() {
        Product p = product(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(InsufficientStockException.class,
                () -> inventoryService.reserve(request("o-2", 1L, 2)));

        assertEquals(1, p.getStockQuantity());
        verify(productRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserve of an unknown product fails with not-found")
    void reserveUnknownProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.reserve(request("o-3", 99L, 1)));
    }

    @Test
    @DisplayName("reserving exactly the remaining stock is allowed and leaves zero")
    void reserveExactlyRemainingStock() {
        Product p = product(2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        inventoryService.reserve(request("o-4", 1L, 2));

        assertEquals(0, p.getStockQuantity());
    }

    @Test
    @DisplayName("release (the saga compensation) puts back exactly what was reserved and marks it RELEASED")
    void releaseRestoresStock() {
        Product p = product(3);
        Reservation reservation = new Reservation("o-5", 1L, 2, ReservationStatus.RESERVED);
        when(reservationRepository.findByOrderIdAndStatus("o-5", ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        ReleaseRequest release = new ReleaseRequest();
        release.setOrderId("o-5");
        inventoryService.release(release);

        assertEquals(5, p.getStockQuantity());
        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
    }

    @Test
    @DisplayName("release with nothing reserved is a harmless no-op")
    void releaseWithNothingReservedIsNoOp() {
        when(reservationRepository.findByOrderIdAndStatus("o-6", ReservationStatus.RESERVED))
                .thenReturn(List.of());

        ReleaseRequest release = new ReleaseRequest();
        release.setOrderId("o-6");
        inventoryService.release(release);

        verify(productRepository, never()).save(any());
    }
}
