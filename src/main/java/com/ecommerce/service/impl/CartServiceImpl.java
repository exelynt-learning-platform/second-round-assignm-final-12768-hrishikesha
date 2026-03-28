package com.ecommerce.service.impl;

import com.ecommerce.dto.CartDto;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedException;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public CartDto.Response getCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartDto.Response addItem(Long userId, CartDto.AddItemRequest request) {
        Cart cart = getCartByUserId(userId);

        Product product = productRepository.findById(request.getProductId())
                .filter(Product::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName(),
                    product.getStockQuantity(), request.getQuantity());
        }

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQty) {
                throw new InsufficientStockException(product.getName(),
                        product.getStockQuantity(), newQty);
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.addItem(newItem);
        }

        cart = cartRepository.save(cart);
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartDto.Response updateItem(Long userId, Long cartItemId, CartDto.UpdateItemRequest request) {
        Cart cart = getCartByUserId(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new UnauthorizedException("You are not allowed to modify this cart item");
        }

        Product product = item.getProduct();
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName(),
                    product.getStockQuantity(), request.getQuantity());
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartDto.Response removeItem(Long userId, Long cartItemId) {
        Cart cart = getCartByUserId(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new UnauthorizedException("You are not allowed to remove this cart item");
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);
        cart = cartRepository.save(cart);

        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cart.clearItems();
        cartRepository.save(cart);
    }

    private Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
    }

    private CartDto.Response mapToResponse(Cart cart) {
        List<CartDto.CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    CartDto.CartItemResponse r = new CartDto.CartItemResponse();
                    r.setCartItemId(item.getId());
                    r.setProductId(item.getProduct().getId());
                    r.setProductName(item.getProduct().getName());
                    r.setProductImageUrl(item.getProduct().getImageUrl());
                    r.setProductPrice(item.getProduct().getPrice());
                    r.setQuantity(item.getQuantity());
                    r.setSubtotal(item.getSubtotal());
                    return r;
                })
                .collect(Collectors.toList());

        CartDto.Response response = new CartDto.Response();
        response.setCartId(cart.getId());
        response.setItems(itemResponses);
        response.setTotalPrice(cart.getTotalPrice());
        response.setItemCount(itemResponses.size());
        response.setUpdatedAt(cart.getUpdatedAt());
        return response;
    }
}