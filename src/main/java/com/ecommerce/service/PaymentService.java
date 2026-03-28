package com.ecommerce.service;

import com.ecommerce.dto.PaymentDto;

import java.math.BigDecimal;

public interface PaymentService {
    void updatePaymentStatus(String paymentIntentId, String status);
}