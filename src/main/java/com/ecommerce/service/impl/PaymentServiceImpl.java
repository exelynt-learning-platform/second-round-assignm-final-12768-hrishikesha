package com.ecommerce.service.impl;

import com.ecommerce.dto.PaymentDto;
import com.ecommerce.exception.PaymentException;
import com.ecommerce.service.PaymentGatewayService;
import com.ecommerce.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentGatewayService {

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    private final PaymentService paymentService;

    public PaymentServiceImpl(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(stripeApiKey)) {
            log.warn("Stripe API key is not configured. Payment processing will be unavailable.");
            return;
        }
        Stripe.apiKey = stripeApiKey;
        log.info("Stripe initialized successfully.");
    }

    @Override
    public PaymentDto.CreatePaymentIntentResponse createPaymentIntent(Long orderId, BigDecimal amount) {
        if (!StringUtils.hasText(stripeApiKey)) {
            throw new PaymentException("Stripe is not configured. Set STRIPE_API_KEY environment variable.");
        }

        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .putMetadata("orderId", String.valueOf(orderId))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            log.info("Created Stripe PaymentIntent {} for order {}", paymentIntent.getId(), orderId);

            return new PaymentDto.CreatePaymentIntentResponse(
                    paymentIntent.getClientSecret(),
                    paymentIntent.getId(),
                    amountInCents,
                    "usd");

        } catch (StripeException e) {
            log.error("Stripe error for order {}: {}", orderId, e.getMessage());
            throw new PaymentException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }

    @Override
    public void handleWebhookEvent(String payload, String sigHeader) {
        if (!StringUtils.hasText(webhookSecret)) {
            throw new PaymentException("Stripe webhook secret is not configured.");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature failed: {}", e.getMessage());
            throw new PaymentException("Invalid webhook signature");
        }

        log.info("Stripe webhook received: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new PaymentException("Failed to deserialize PaymentIntent"));
                paymentService.updatePaymentStatus(paymentIntent.getId(), "succeeded");
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new PaymentException("Failed to deserialize PaymentIntent"));
                paymentService.updatePaymentStatus(paymentIntent.getId(), "payment_failed");
            }
            default -> log.info("Unhandled Stripe event: {}", event.getType());
        }
    }
}