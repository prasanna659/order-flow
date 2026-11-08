package com.orderflow.payment.service;

import com.orderflow.payment.dto.ChargeRequest;
import com.orderflow.payment.dto.ChargeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceTest {

    private final PaymentService paymentService = new PaymentService();

    private ChargeRequest request(String orderId) {
        ChargeRequest r = new ChargeRequest();
        r.setOrderId(orderId);
        r.setAmount(new BigDecimal("25.00"));
        return r;
    }

    @Test
    @DisplayName("every charge gets a transaction id and is recorded against its order")
    void chargeIsRecorded() {
        ChargeResponse response = paymentService.charge(request("order-a"));

        assertNotNull(response.getTransactionId());
        assertEquals(response, paymentService.getTransaction("order-a"));
    }

    @Test
    @DisplayName("unknown order has no transaction")
    void unknownOrderHasNoTransaction() {
        assertNull(paymentService.getTransaction("never-charged"));
    }

    @Test
    @DisplayName("the simulated failure rate really produces both approvals and declines")
    void producesBothOutcomes() {
        int approved = 0;
        int declined = 0;
        // ~15% decline rate: the chance of seeing zero declines in 2000 tries is effectively nil.
        for (int i = 0; i < 2000; i++) {
            if (paymentService.charge(request("order-" + i)).isSuccess()) {
                approved++;
            } else {
                declined++;
            }
        }
        assertTrue(approved > 0, "expected some approvals");
        assertTrue(declined > 0, "expected some declines");
        assertTrue(declined < approved, "declines should be the minority");
    }
}
