package com.ecommerce.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "stripe")
@Getter
@Setter
public class StripeProperties {

    private String apiKey;
    private String webhookSecret;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(apiKey) || apiKey.equals("sk_test_YOUR_STRIPE_KEY_HERE")) {
            throw new IllegalStateException(
                    "Stripe API key is not configured. Set STRIPE_API_KEY environment variable.");
        }
        if (!StringUtils.hasText(webhookSecret) || webhookSecret.equals("whsec_YOUR_WEBHOOK_SECRET_HERE")) {
            throw new IllegalStateException(
                    "Stripe webhook secret is not configured. Set STRIPE_WEBHOOK_SECRET environment variable.");
        }
    }
}