package com.ecommerce.service;

import com.ecommerce.dto.CartDto;

public interface CartService {
    CartDto.Response getCart(Long userId);
    CartDto.Response addItem(Long userId, CartDto.AddItemRequest request);
    CartDto.Response updateItem(Long userId, Long cartItemId, CartDto.UpdateItemRequest request);
    CartDto.Response removeItem(Long userId, Long cartItemId);
    void clearCart(Long userId);
}