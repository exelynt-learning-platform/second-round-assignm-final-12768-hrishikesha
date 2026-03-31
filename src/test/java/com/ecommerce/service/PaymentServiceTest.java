package com.ecommerce.service;

import com.ecommerce.dto.PaymentDto;
import com.ecommerce.exception.PaymentException;
import com.ecommerce.service.impl.OrderServiceImpl;
import com.ecommerce.service.impl.PaymentServiceImpl;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderServiceImpl orderService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void handleWebhookEvent_invalidSignature_throwsPaymentException() {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "test_secret");

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new SignatureVerificationException("Invalid signature", "sig"));

            assertThatThrownBy(() -> paymentService.handleWebhookEvent("payload", "bad-sig"))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("Invalid webhook signature");
        }
    }

    @Test
    void createPaymentIntent_stripeNotConfigured_throwsPaymentException() {
        ReflectionTestUtils.setField(paymentService, "stripeApiKey", "");

        assertThatThrownBy(() -> paymentService.createPaymentIntent(1L, new BigDecimal("100.00")))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Stripe is not configured");
    }

    @Test
    void handleWebhookEvent_webhookSecretNotConfigured_throwsPaymentException() {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "");

        assertThatThrownBy(() -> paymentService.handleWebhookEvent("payload", "sig"))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("webhook secret is not configured");
    }
}