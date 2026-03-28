package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDto {

    @Data
    public static class CreateRequest {

        @NotBlank(message = "Product name is required")
        private String name;

        private String description;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        private BigDecimal price;

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        private Integer stockQuantity;

        private String imageUrl;
        private String category;
    }

    @Data
    public static class UpdateRequest {

        private String name;
        private String description;

        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        private BigDecimal price;

        @Min(value = 0, message = "Stock quantity cannot be negative")
        private Integer stockQuantity;

        private String imageUrl;
        private String category;
        private Boolean active;
    }

    @Data
    public static class Response {

        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stockQuantity;
        private String imageUrl;
        private String category;
        private Boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}