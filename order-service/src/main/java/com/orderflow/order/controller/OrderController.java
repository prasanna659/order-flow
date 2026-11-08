package com.orderflow.order.controller;

import com.orderflow.order.dto.OrderItemDto;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.PageResponse;
import com.orderflow.order.dto.PlaceOrderRequest;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.ForbiddenException;
import com.orderflow.order.exception.ResourceNotFoundException;
import com.orderflow.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Every endpoint requires the X-User-Id header. The API gateway sets it after
 * verifying the JWT, so users can only reach their own orders. Requests that
 * arrive without it (for example, straight to this service) are rejected.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    static final String USER_ID_HEADER = "X-User-Id";

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Returns 202 Accepted, not 201 Created: the order exists but the saga
    // hasn't finished. The frontend polls GET /{id} until the status becomes
    // CONFIRMED or CANCELLED.
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestHeader(value = USER_ID_HEADER, required = false) String callerHeader,
            @Valid @RequestBody PlaceOrderRequest request) {
        Long callerId = requireCaller(callerHeader);
        assertSameUser(callerId, request.getUserId());
        Order order = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader(value = USER_ID_HEADER, required = false) String callerHeader,
            @PathVariable String id) {
        Long callerId = requireCaller(callerHeader);
        Order order = orderService.getOrder(id);
        // Answer 404 (not 403) for someone else's order so IDs can't be probed.
        if (order.getUserId() == null || !order.getUserId().equals(callerId)) {
            throw new ResourceNotFoundException("ORDER_NOT_FOUND", "Order not found: " + id);
        }
        return ResponseEntity.ok(toResponse(order));
    }

    @GetMapping("/user/{userId}")
    public List<OrderResponse> getOrdersForUser(
            @RequestHeader(value = USER_ID_HEADER, required = false) String callerHeader,
            @PathVariable Long userId) {
        assertSameUser(requireCaller(callerHeader), userId);
        return orderService.getOrdersForUser(userId).stream().map(OrderResponse::from).toList();
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> getOrders(
            @RequestHeader(value = USER_ID_HEADER, required = false) String callerHeader,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status) {
        assertSameUser(requireCaller(callerHeader), userId);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<Order> ordersPage = orderService.getOrdersForUserPaginated(userId, status, safePage, safeSize);
        return ResponseEntity.ok(PageResponse.of(ordersPage.map(this::toResponse)));
    }

    private OrderResponse toResponse(Order order) {
        try {
            List<OrderItemDto> items = orderService.getOrderItems(order);
            return OrderResponse.fromWithItems(order, items);
        } catch (Exception e) {
            return OrderResponse.from(order);
        }
    }

    private Long requireCaller(String header) {
        if (header == null || header.isBlank()) {
            throw new ForbiddenException("MISSING_IDENTITY", "Missing caller identity");
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            throw new ForbiddenException("INVALID_IDENTITY", "Invalid caller identity");
        }
    }

    private void assertSameUser(Long callerId, Long requestedUserId) {
        if (requestedUserId == null || !requestedUserId.equals(callerId)) {
            throw new ForbiddenException("FORBIDDEN", "You can only access your own orders");
        }
    }
}
