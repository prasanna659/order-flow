package com.orderflow.payment.controller;

import com.orderflow.payment.dto.ChargeRequest;
import com.orderflow.payment.dto.ChargeResponse;
import com.orderflow.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/charge")
    public ResponseEntity<ChargeResponse> charge(@RequestBody ChargeRequest request) {
        return ResponseEntity.ok(paymentService.charge(request));
    }

    @GetMapping("/transactions/{orderId}")
    public ResponseEntity<ChargeResponse> getTransaction(@PathVariable String orderId) {
        ChargeResponse response = paymentService.getTransaction(orderId);
        return response == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(response);
    }
}
