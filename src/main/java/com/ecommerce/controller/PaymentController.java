package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.exception.PaymentException;
import com.ecommerce.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentGatewayService paymentGatewayService;

    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            paymentGatewayService.handleWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok(ApiResponse.success("Webhook processed", null));
        } catch (PaymentException e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}