package com.orderflow.order.controller;

import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.PageResponse;
import com.orderflow.order.dto.PlaceOrderRequest;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.ResourceNotFoundException;
import com.orderflow.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Returns 202 Accepted, not 201 Created -- the order exists but the
    // saga hasn't finished. The frontend is expected to poll GET /{id}
    // until status becomes CONFIRMED or CANCELLED.
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        Order order = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        Order order = orderService.getOrder(id);
        try {
            List<com.orderflow.order.dto.OrderItemDto> items = orderService.getOrderItems(order);
            return ResponseEntity.ok(OrderResponse.fromWithItems(order, items));
        } catch (Exception e) {
            return ResponseEntity.ok(OrderResponse.from(order));
        }
    }

    @GetMapping("/user/{userId}")
    public List<OrderResponse> getOrdersForUser(@PathVariable Long userId) {
        return orderService.getOrdersForUser(userId).stream().map(OrderResponse::from).toList();
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> getOrders(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status) {
        
        // Authorization check: ensure user can only see their own orders
        // In a real app, this would come from JWT token validation
        Page<Order> ordersPage = orderService.getOrdersForUserPaginated(userId, status, page, size);
        Page<OrderResponse> responsePage = ordersPage.map(order -> {
            try {
                List<com.orderflow.order.dto.OrderItemDto> items = orderService.getOrderItems(order);
                return OrderResponse.fromWithItems(order, items);
            } catch (Exception e) {
                return OrderResponse.from(order);
            }
        });
        return ResponseEntity.ok(PageResponse.of(responsePage));
    }
}
