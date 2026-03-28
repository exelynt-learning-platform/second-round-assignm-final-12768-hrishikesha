package com.ecommerce.service;

import com.ecommerce.dto.OrderDto;
import com.ecommerce.dto.PaymentDto;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentService paymentService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Cart cart;
    private Product product;
    private OrderDto.CreateRequest createRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .role(User.Role.ROLE_USER)
                .build();

        product = Product.builder()
                .id(10L)
                .name("Widget")
                .price(new BigDecimal("50.00"))
                .stockQuantity(20)
                .active(true)
                .build();

        CartItem cartItem = CartItem.builder()
                .product(product)
                .quantity(2)
                .build();

        cart = Cart.builder().id(1L).user(user).build();
        cart.getItems().add(cartItem);
        cartItem.setCart(cart);

        createRequest = new OrderDto.CreateRequest();
        createRequest.setShippingName("Alice Smith");
        createRequest.setShippingAddress("1 Main St");
        createRequest.setShippingCity("Springfield");
        createRequest.setShippingZip("12345");
        createRequest.setShippingCountry("US");
    }

    @Test
    void createOrder_validCart_orderCreatedAndStockDecremented() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        PaymentDto.CreatePaymentIntentResponse mockPayment =
                new PaymentDto.CreatePaymentIntentResponse("pi_secret", "pi_123", 10000L, "usd");
        when(paymentService.createPaymentIntent(any(), any())).thenReturn(mockPayment);

        Order savedOrder = Order.builder()
                .id(1L)
                .user(user)
                .totalPrice(new BigDecimal("100.00"))
                .shippingName("Alice Smith")
                .shippingAddress("1 Main St")
                .shippingCity("Springfield")
                .shippingZip("12345")
                .shippingCountry("US")
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.PLACED)
                .stripeClientSecret("pi_secret")
                .stripePaymentIntentId("pi_123")
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        OrderDto.Response response = orderService.createOrder(1L, createRequest);

        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(response.getOrderStatus()).isEqualTo("PLACED");
        assertThat(response.getStripeClientSecret()).isEqualTo("pi_secret");
        assertThat(product.getStockQuantity()).isEqualTo(18);
        verify(productRepository).save(product);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void createOrder_emptyCart_throwsBadRequestException() {
        cart.clearItems();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder(1L, createRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void createOrder_insufficientStock_throwsInsufficientStockException() {
        product.setStockQuantity(1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrder(1L, createRequest))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Widget");
    }

    @Test
    void createOrder_inactiveProduct_throwsBadRequestException() {
        product.setActive(false);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrder(1L, createRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void cancelOrder_placedOrder_cancelledAndStockRestored() {
        OrderItem orderItem = OrderItem.builder()
                .product(product)
                .productName("Widget")
                .priceAtPurchase(new BigDecimal("50.00"))
                .quantity(2)
                .build();

        Order order = Order.builder()
                .id(5L)
                .user(user)
                .orderStatus(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .totalPrice(new BigDecimal("100.00"))
                .shippingName("Alice")
                .shippingAddress("1 St")
                .shippingCity("City")
                .shippingZip("00000")
                .shippingCountry("US")
                .build();

        order.getItems().add(orderItem);
        orderItem.setOrder(order);

        when(orderRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(order));
        when(productRepository.save(any())).thenReturn(product);
        when(orderRepository.save(any())).thenReturn(order);

        OrderDto.Response response = orderService.cancelOrder(1L, 5L);

        assertThat(response.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(product.getStockQuantity()).isEqualTo(22);
        verify(productRepository).save(product);
    }

    @Test
    void cancelOrder_shippedOrder_throwsBadRequestException() {
        Order order = Order.builder()
                .id(5L)
                .user(user)
                .orderStatus(Order.OrderStatus.SHIPPED)
                .paymentStatus(Order.PaymentStatus.PAID)
                .build();

        when(orderRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("shipped");
    }

    @Test
    void cancelOrder_alreadyCancelled_throwsBadRequestException() {
        Order order = Order.builder()
                .id(5L)
                .user(user)
                .orderStatus(Order.OrderStatus.CANCELLED)
                .paymentStatus(Order.PaymentStatus.REFUNDED)
                .build();

        when(orderRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void updatePaymentStatus_succeeded_setsOrderPaidAndConfirmed() {
        Order order = Order.builder()
                .id(3L)
                .stripePaymentIntentId("pi_abc")
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.PLACED)
                .build();

        when(orderRepository.findByStripePaymentIntentId("pi_abc")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.updatePaymentStatus("pi_abc", "succeeded");

        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.PAID);
        assertThat(order.getOrderStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    void updatePaymentStatus_paymentFailed_setsOrderFailed() {
        Order order = Order.builder()
                .id(3L)
                .stripePaymentIntentId("pi_xyz")
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.PLACED)
                .build();

        when(orderRepository.findByStripePaymentIntentId("pi_xyz")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.updatePaymentStatus("pi_xyz", "payment_failed");

        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.FAILED);
    }

    @Test
    void updatePaymentStatus_unknownIntent_throwsResourceNotFoundException() {
        when(orderRepository.findByStripePaymentIntentId("pi_unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updatePaymentStatus("pi_unknown", "succeeded"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrderById_orderBelongsToUser_returnsOrder() {
        Order order = Order.builder()
                .id(1L)
                .user(user)
                .totalPrice(new BigDecimal("100.00"))
                .shippingName("Alice")
                .shippingAddress("1 St")
                .shippingCity("City")
                .shippingZip("00000")
                .shippingCountry("US")
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.PLACED)
                .build();

        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));

        OrderDto.Response response = orderService.getOrderById(1L, 1L);

        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
    }

    @Test
    void getOrderById_orderNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}