package com.ecommerce.service;

import com.ecommerce.dto.PaymentDto;

import java.math.BigDecimal;

public interface PaymentGatewayService {
    PaymentDto.CreatePaymentIntentResponse createPaymentIntent(Long orderId, BigDecimal amount);
    void handleWebhookEvent(String payload, String sigHeader);
}