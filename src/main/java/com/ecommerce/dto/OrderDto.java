package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    @Data
    public static class CreateRequest {

        @NotBlank(message = "Shipping name is required")
        private String shippingName;

        @NotBlank(message = "Shipping address is required")
        private String shippingAddress;

        @NotBlank(message = "City is required")
        private String shippingCity;

        @NotBlank(message = "ZIP code is required")
        private String shippingZip;

        @NotBlank(message = "Country is required")
        private String shippingCountry;

        private String shippingPhone;
    }

    @Data
    public static class OrderItemResponse {

        private Long orderItemId;
        private Long productId;
        private String productName;
        private BigDecimal priceAtPurchase;
        private Integer quantity;
        private BigDecimal subtotal;
    }

    @Data
    public static class Response {

        private Long orderId;
        private List<OrderItemResponse> items;
        private BigDecimal totalPrice;
        private String shippingName;
        private String shippingAddress;
        private String shippingCity;
        private String shippingZip;
        private String shippingCountry;
        private String paymentStatus;
        private String orderStatus;
        private String stripeClientSecret;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class PagedResponse {

        private List<Response> orders;
        private int currentPage;
        private long totalItems;
        private int totalPages;
    }
}