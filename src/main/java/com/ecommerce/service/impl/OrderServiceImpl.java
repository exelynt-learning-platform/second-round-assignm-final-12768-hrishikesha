package com.ecommerce.service.impl;

import com.ecommerce.dto.OrderDto;
import com.ecommerce.dto.PaymentDto;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public OrderDto.Response createOrder(Long userId, OrderDto.CreateRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot create order: cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", cartItem.getProduct().getId()));

            if (!product.getActive()) {
                throw new BadRequestException("Product is no longer available: " + product.getName());
            }

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(product.getName(), product.getStockQuantity(), cartItem.getQuantity());
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .priceAtPurchase(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();

            orderItems.add(orderItem);
            totalPrice = totalPrice.add(orderItem.getSubtotal());
        }

        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .shippingName(request.getShippingName())
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingZip(request.getShippingZip())
                .shippingCountry(request.getShippingCountry())
                .shippingPhone(request.getShippingPhone())
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.PLACED)
                .build();

        order = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        order = orderRepository.save(order);

        try {
            PaymentDto.CreatePaymentIntentResponse paymentIntent = paymentService.createPaymentIntent(order.getId(), totalPrice);
            order.setStripePaymentIntentId(paymentIntent.getPaymentIntentId());
            order.setStripeClientSecret(paymentIntent.getClientSecret());
            order = orderRepository.save(order);
        } catch (Exception e) {
            log.error("Stripe PaymentIntent failed for order {}: {}", order.getId(), e.getMessage());
        }

        cart.clearItems();
        cartRepository.save(cart);

        log.info("Created order {} for user {}", order.getId(), userId);
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto.Response getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto.PagedResponse getUserOrders(Long userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserId(userId, pageable);
        return buildPagedResponse(page);
    }

    @Override
    @Transactional
    public OrderDto.Response cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getOrderStatus() == Order.OrderStatus.SHIPPED ||
                order.getOrderStatus() == Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel an order that is already shipped or delivered");
        }

        if (order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        order = orderRepository.save(order);

        log.info("Cancelled order {} for user {}", orderId, userId);
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public void updatePaymentStatus(String paymentIntentId, String status) {
        Order order = orderRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found for paymentIntentId: " + paymentIntentId));

        switch (status) {
            case "succeeded" -> {
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                order.setOrderStatus(Order.OrderStatus.CONFIRMED);
            }
            case "payment_failed" -> order.setPaymentStatus(Order.PaymentStatus.FAILED);
            default -> log.warn("Unhandled payment status: {}", status);
        }

        orderRepository.save(order);
        log.info("Updated payment status for order {} to {}", order.getId(), status);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto.PagedResponse getAllOrders(Pageable pageable) {
        Page<Order> page = orderRepository.findAll(pageable);
        return buildPagedResponse(page);
    }

    @Override
    @Transactional
    public OrderDto.Response updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        try {
            order.setOrderStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status: " + status);
        }

        order = orderRepository.save(order);
        return mapToResponse(order);
    }

    private OrderDto.Response mapToResponse(Order order) {
        List<OrderDto.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    OrderDto.OrderItemResponse r = new OrderDto.OrderItemResponse();
                    r.setOrderItemId(item.getId());
                    r.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                    r.setProductName(item.getProductName());
                    r.setPriceAtPurchase(item.getPriceAtPurchase());
                    r.setQuantity(item.getQuantity());
                    r.setSubtotal(item.getSubtotal());
                    return r;
                })
                .collect(Collectors.toList());

        OrderDto.Response response = new OrderDto.Response();
        response.setOrderId(order.getId());
        response.setItems(itemResponses);
        response.setTotalPrice(order.getTotalPrice());
        response.setShippingName(order.getShippingName());
        response.setShippingAddress(order.getShippingAddress());
        response.setShippingCity(order.getShippingCity());
        response.setShippingZip(order.getShippingZip());
        response.setShippingCountry(order.getShippingCountry());
        response.setPaymentStatus(order.getPaymentStatus().name());
        response.setOrderStatus(order.getOrderStatus().name());
        response.setStripeClientSecret(order.getStripeClientSecret());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }

    private OrderDto.PagedResponse buildPagedResponse(Page<Order> page) {
        OrderDto.PagedResponse pagedResponse = new OrderDto.PagedResponse();
        pagedResponse.setOrders(page.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()));
        pagedResponse.setCurrentPage(page.getNumber());
        pagedResponse.setTotalItems(page.getTotalElements());
        pagedResponse.setTotalPages(page.getTotalPages());
        return pagedResponse;
    }
}