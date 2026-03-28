package com.ecommerce.dto;

import lombok.Data;

public class PaymentDto {

    @Data
    public static class CreatePaymentIntentResponse {

        private String clientSecret;
        private String paymentIntentId;
        private Long amount;
        private String currency;

        public CreatePaymentIntentResponse(String clientSecret, String paymentIntentId, Long amount, String currency) {
            this.clientSecret = clientSecret;
            this.paymentIntentId = paymentIntentId;
            this.amount = amount;
            this.currency = currency;
        }
    }
}