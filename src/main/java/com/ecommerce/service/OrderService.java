package com.ecommerce.service;

import com.ecommerce.dto.OrderDto;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderDto.Response createOrder(Long userId, OrderDto.CreateRequest request);
    OrderDto.Response getOrderById(Long userId, Long orderId);
    OrderDto.PagedResponse getUserOrders(Long userId, Pageable pageable);
    OrderDto.Response cancelOrder(Long userId, Long orderId);
    void updatePaymentStatus(String paymentIntentId, String status);
    OrderDto.PagedResponse getAllOrders(Pageable pageable);
    OrderDto.Response updateOrderStatus(Long orderId, String status);
}