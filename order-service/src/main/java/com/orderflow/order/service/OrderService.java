package com.orderflow.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.dto.OrderItemDto;
import com.orderflow.order.dto.PlaceOrderRequest;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.*;
import com.orderflow.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderService(OrderRepository orderRepository, OrderSagaOrchestrator sagaOrchestrator) {
        this.orderRepository = orderRepository;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    /**
     * STEP 1 of the saga — synchronous.
     * Creates the order as PENDING, returns immediately with the order ID.
     * The client polls GET /api/orders/{id} to watch status transitions.
     */
    public Order placeOrder(PlaceOrderRequest request) {
        BigDecimal total = request.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(request.getUserId());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        try {
            order.setItemsJson(objectMapper.writeValueAsString(request.getItems()));
        } catch (Exception ignored) {}

        order = orderRepository.save(order);

        // Capture MDC values NOW, on the request thread, before handing off
        // to the async executor (MDC is thread-local and won't transfer automatically).
        String capturedRequestId = MDC.get("requestId");
        String capturedTraceId   = MDC.get("traceId");

        // Goes through the Spring proxy (a different bean), so it really runs async.
        sagaOrchestrator.runSaga(order.getId(), request.getItems(), request.getUserEmail(),
                request.getUsername(), capturedRequestId, capturedTraceId);

        return order;
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order not found: " + orderId));
    }

    public List<Order> getOrdersForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public org.springframework.data.domain.Page<Order> getOrdersForUserPaginated(
            Long userId, OrderStatus status, int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        }
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<OrderItemDto> getOrderItems(Order order) {
        try {
            if (order.getItemsJson() != null && !order.getItemsJson().isEmpty()) {
                return objectMapper.readValue(order.getItemsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, OrderItemDto.class));
            }
        } catch (Exception e) {
            log.error("Failed to parse order items for order {}: {}", order.getId(), e.getMessage());
        }
        return List.of();
    }
}
