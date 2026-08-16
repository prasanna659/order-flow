package com.orderflow.payment.service;

import com.orderflow.payment.dto.ChargeRequest;
import com.orderflow.payment.dto.ChargeResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    // In-memory transaction log. A real payment-service wouldn't own a
    // ledger like this either -- it would delegate to Stripe/Razorpay and
    // store only a reference ID. This is here so the saga has something
    // real to inspect and so /api/payments/transactions has data to show.
    private final Map<String, ChargeResponse> transactions = new ConcurrentHashMap<>();

    // Simulated ~15% decline rate so the compensating-transaction path in
    // order-service actually gets exercised during a demo, not just the
    // happy path. In an interview you can point at this line and say
    // "I made failures reproducible enough to demo the rollback live."
    private static final double FAILURE_RATE = 0.15;

    public ChargeResponse charge(ChargeRequest request) {
        boolean success = Math.random() > FAILURE_RATE;
        String transactionId = UUID.randomUUID().toString();

        ChargeResponse response = success
                ? new ChargeResponse(true, transactionId, "Payment approved")
                : new ChargeResponse(false, transactionId, "Payment declined by issuing bank");

        transactions.put(request.getOrderId(), response);
        return response;
    }

    public ChargeResponse getTransaction(String orderId) {
        return transactions.get(orderId);
    }
}
