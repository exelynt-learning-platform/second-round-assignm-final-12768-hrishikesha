package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDto {

    @Data
    public static class RegisterRequest {

        @NotBlank(message = "Name is required")
        private String name;

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        private String phone;
        private String address;
    }

    @Data
    public static class LoginRequest {

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class AuthResponse {

        private String accessToken;
        private String tokenType = "Bearer";
        private Long userId;
        private String name;
        private String email;
        private String role;

        public AuthResponse(String accessToken, Long userId, String name, String email, String role) {
            this.accessToken = accessToken;
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
        }
    }
}