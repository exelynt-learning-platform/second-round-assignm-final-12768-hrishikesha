package com.ecommerce.service;

public interface PaymentService {
    void updatePaymentStatus(String paymentIntentId, String status);
}