package com.ecommerce.service;

import com.ecommerce.dto.ProductDto;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Headphones")
                .description("Noise cancelling")
                .price(new BigDecimal("199.99"))
                .stockQuantity(50)
                .category("Electronics")
                .imageUrl("http://example.com/img.jpg")
                .active(true)
                .build();
    }

    @Test
    void createProduct_validRequest_returnsResponse() {
        ProductDto.CreateRequest request = new ProductDto.CreateRequest();
        request.setName("Headphones");
        request.setDescription("Noise cancelling");
        request.setPrice(new BigDecimal("199.99"));
        request.setStockQuantity(50);
        request.setCategory("Electronics");
        request.setImageUrl("http://example.com/img.jpg");

        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDto.Response response = productService.createProduct(request);

        assertThat(response.getName()).isEqualTo("Headphones");
        assertThat(response.getPrice()).isEqualByComparingTo("199.99");
        assertThat(response.getStockQuantity()).isEqualTo(50);
        assertThat(response.getActive()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getProductById_activeProduct_returnsResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDto.Response response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Headphones");
    }

    @Test
    void getProductById_inactiveProduct_throwsResourceNotFoundException() {
        product.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProductById_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllProducts_returnsPageOfProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(productRepository.findByActiveTrue(pageable)).thenReturn(productPage);

        Page<ProductDto.Response> result = productService.getAllProducts(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Headphones");
    }

    @Test
    void updateProduct_partialUpdate_onlyUpdatesProvidedFields() {
        ProductDto.UpdateRequest request = new ProductDto.UpdateRequest();
        request.setPrice(new BigDecimal("149.99"));
        request.setStockQuantity(30);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDto.Response response = productService.updateProduct(1L, request);

        assertThat(product.getPrice()).isEqualByComparingTo("149.99");
        assertThat(product.getStockQuantity()).isEqualTo(30);
        assertThat(product.getName()).isEqualTo("Headphones");
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ProductDto.UpdateRequest request = new ProductDto.UpdateRequest();
        request.setPrice(new BigDecimal("99.99"));

        assertThatThrownBy(() -> productService.updateProduct(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteProduct_activeProduct_softDeletesIt() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(1L);

        assertThat(product.getActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchProducts_returnsMatchingProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(productRepository.searchProducts("headphone", pageable)).thenReturn(productPage);

        Page<ProductDto.Response> result = productService.searchProducts("headphone", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Headphones");
    }
}