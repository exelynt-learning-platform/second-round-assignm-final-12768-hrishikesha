# E-Commerce Backend

A full-featured e-commerce backend built with Spring Boot, Spring Security, JWT authentication, and Stripe payment integration.

## Tech Stack

- Java 17
- Spring Boot 3.2.0
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Stripe Payment Gateway
- Lombok
- Maven

## Features

- User registration and login with JWT authentication
- Role-based authorization (ADMIN / USER)
- BCrypt password hashing
- Product management with search and category filter
- Cart management with ownership validation
- Order processing with automatic stock deduction
- Stripe payment gateway integration with webhook support
- Global error handling with meaningful messages
- Input validation on all endpoints
- 25 unit tests covering all core components

## Project Structure

```
src/main/java/com/ecommerce/
├── BackendApplication.java
├── config/
│   ├── SecurityConfig.java
│   └── DataInitializer.java
├── controller/
│   ├── AuthController.java
│   ├── ProductController.java
│   ├── CartController.java
│   ├── OrderController.java
│   └── PaymentController.java
├── dto/
│   ├── ApiResponse.java
│   ├── AuthDto.java
│   ├── ProductDto.java
│   ├── CartDto.java
│   ├── OrderDto.java
│   └── PaymentDto.java
├── entity/
│   ├── User.java
│   ├── Product.java
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Order.java
│   └── OrderItem.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   ├── EmailAlreadyExistsException.java
│   ├── UnauthorizedException.java
│   ├── InsufficientStockException.java
│   └── PaymentException.java
├── repository/
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CartRepository.java
│   ├── CartItemRepository.java
│   └── OrderRepository.java
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserDetailsServiceImpl.java
│   └── SecurityUtils.java
└── service/
    ├── AuthService.java
    ├── ProductService.java
    ├── CartService.java
    ├── OrderService.java
    ├── PaymentService.java
    └── impl/
        ├── AuthServiceImpl.java
        ├── ProductServiceImpl.java
        ├── CartServiceImpl.java
        ├── OrderServiceImpl.java
        └── PaymentServiceImpl.java
```

## Prerequisites

- Java 17+
- Maven
- PostgreSQL

## Database Setup

```sql
CREATE DATABASE ecommercedb;
```

## Configuration

Update `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommercedb
spring.datasource.username=postgres
spring.datasource.password=your_password_here

stripe.api.key=sk_test_YOUR_STRIPE_KEY_HERE
stripe.webhook.secret=whsec_YOUR_WEBHOOK_SECRET_HERE
```

## Run the Application

```bash
./mvnw spring-boot:run
```

## Run Tests

```bash
./mvnw test
```

All 25 tests should pass.

## Default Seeded Accounts

| Role  | Email             | Password    |
|-------|-------------------|-------------|
| Admin | admin@shop.com    | Admin@1234  |
| User  | user@shop.com     | User@1234   |

## API Endpoints

### Authentication
| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| POST | /api/auth/register | Public | Register new user |
| POST | /api/auth/login | Public | Login and get JWT token |

### Products
| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| GET | /api/products | Public | Get all products (paginated) |
| GET | /api/products/{id} | Public | Get product by ID |
| GET | /api/products?search=keyword | Public | Search products |
| GET | /api/products?category=name | Public | Filter by category |
| POST | /api/products | Admin | Create product |
| PUT | /api/products/{id} | Admin | Update product |
| DELETE | /api/products/{id} | Admin | Soft delete product |

### Cart
| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| GET | /api/cart | User | Get current user cart |
| POST | /api/cart/items | User | Add item to cart |
| PUT | /api/cart/items/{id} | User | Update item quantity |
| DELETE | /api/cart/items/{id} | User | Remove item from cart |
| DELETE | /api/cart | User | Clear entire cart |

### Orders
| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| POST | /api/orders | User | Place order from cart |
| GET | /api/orders | User | Get my orders |
| GET | /api/orders/{id} | User | Get order by ID |
| PATCH | /api/orders/{id}/cancel | User | Cancel order |
| GET | /api/orders/admin/all | Admin | Get all orders |
| PATCH | /api/orders/admin/{id}/status | Admin | Update order status |

### Payments
| Method | URL | Access | Description |
|--------|-----|--------|-------------|
| POST | /api/payments/webhook | Stripe | Handle Stripe webhook events |

## Authentication

All protected endpoints require a Bearer token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

Get the token from the login or register response.

## Example Request Flow

### 1. Register
```bash
POST /api/auth/register
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "Password@123"
}
```

### 2. Add to Cart
```bash
POST /api/cart/items
Authorization: Bearer <token>
{
  "productId": 1,
  "quantity": 2
}
```

### 3. Place Order
```bash
POST /api/orders
Authorization: Bearer <token>
{
  "shippingName": "John Doe",
  "shippingAddress": "123 Main Street",
  "shippingCity": "Mumbai",
  "shippingZip": "400001",
  "shippingCountry": "India"
}
```

## API Response Format

All responses follow this structure:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { },
  "timestamp": "2026-03-28T11:00:00"
}
```

## Error Response Format

```json
{
  "success": false,
  "message": "Detailed error message here",
  "data": null,
  "timestamp": "2026-03-28T11:00:00"
}
```

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict |
| 500 | Internal Server Error |

## Database Relationships

- User → Orders (One-to-Many)
- User → Cart (One-to-One)
- Cart → CartItems → Products (Many-to-Many via CartItem)
- Order → OrderItems → Products (Many-to-Many via OrderItem)

## Payment Flow

1. User places order → Stripe PaymentIntent created automatically
2. Frontend receives `stripeClientSecret` from order response
3. Frontend confirms payment using Stripe.js
4. Stripe sends webhook event to `/api/payments/webhook`
5. Backend updates order `paymentStatus` to PAID and `orderStatus` to CONFIRMED
