package com.orderflow.order.controller;

import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.GlobalExceptionHandler;
import com.orderflow.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ownership rules: a caller may only see and create their own orders. */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private static final String HEADER = "X-User-Id";

    @Mock private OrderService orderService;
    @InjectMocks private OrderController controller;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Order orderOwnedBy(long userId) {
        Order o = new Order();
        o.setId("o-1");
        o.setUserId(userId);
        o.setTotalAmount(new BigDecimal("10.00"));
        o.setStatus(OrderStatus.CONFIRMED);
        o.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        o.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return o;
    }

    private static final String BODY_FOR_USER_1 =
            "{\"userId\":1,\"items\":[{\"productId\":1,\"productName\":\"Keyboard\",\"quantity\":1,\"price\":10.00}]}";

    @Test
    @DisplayName("request without the gateway identity header is rejected with 403")
    void missingHeaderIsForbidden() throws Exception {
        mvc.perform(get("/api/orders").param("userId", "1"))
                .andExpect(status().isForbidden());
        verify(orderService, never()).getOrdersForUserPaginated(any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("listing another user's orders is rejected with 403")
    void listingOtherUsersOrdersIsForbidden() throws Exception {
        mvc.perform(get("/api/orders").param("userId", "2").header(HEADER, "1"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/user/2").header(HEADER, "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("listing your own orders works")
    void listingOwnOrdersWorks() throws Exception {
        when(orderService.getOrdersForUserPaginated(eq(1L), isNull(), eq(0), eq(10)))
                .thenReturn(new PageImpl<>(List.of(orderOwnedBy(1L))));

        mvc.perform(get("/api/orders").param("userId", "1").header(HEADER, "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("fetching someone else's order by id returns 404, so ids cannot be probed")
    void otherUsersOrderLooksNotFound() throws Exception {
        when(orderService.getOrder("o-1")).thenReturn(orderOwnedBy(2L));

        mvc.perform(get("/api/orders/o-1").header(HEADER, "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("fetching your own order by id works")
    void ownOrderIsReturned() throws Exception {
        when(orderService.getOrder("o-1")).thenReturn(orderOwnedBy(1L));
        when(orderService.getOrderItems(any(Order.class))).thenReturn(List.of());

        mvc.perform(get("/api/orders/o-1").header(HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("o-1"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("placing an order for a different userId than the caller is rejected")
    void placingOrderForSomeoneElseIsForbidden() throws Exception {
        mvc.perform(post("/api/orders")
                        .header(HEADER, "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_FOR_USER_1))
                .andExpect(status().isForbidden());
        verify(orderService, never()).placeOrder(any());
    }

    @Test
    @DisplayName("placing your own order returns 202 Accepted")
    void placingOwnOrderIsAccepted() throws Exception {
        Order pending = orderOwnedBy(1L);
        pending.setStatus(OrderStatus.PENDING);
        when(orderService.placeOrder(any())).thenReturn(pending);

        mvc.perform(post("/api/orders")
                        .header(HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_FOR_USER_1))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("a non-numeric identity header is rejected")
    void garbageHeaderIsForbidden() throws Exception {
        mvc.perform(get("/api/orders/o-1").header(HEADER, "not-a-number"))
                .andExpect(status().isForbidden());
    }
}
