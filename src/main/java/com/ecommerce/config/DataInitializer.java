package com.ecommerce.config;

import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedSampleUser();
        seedProducts();
        log.info("Data initialization complete");
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail("admin@shop.com")) return;

        User admin = User.builder()
                .name("Admin User")
                .email("admin@shop.com")
                .password(passwordEncoder.encode("Admin@1234"))
                .role(User.Role.ROLE_ADMIN)
                .build();
        admin = userRepository.save(admin);
        cartRepository.save(Cart.builder().user(admin).build());
        log.info("Seeded admin -> admin@shop.com / Admin@1234");
    }

    private void seedSampleUser() {
        if (userRepository.existsByEmail("user@shop.com")) return;

        User user = User.builder()
                .name("Sample User")
                .email("user@shop.com")
                .password(passwordEncoder.encode("User@1234"))
                .role(User.Role.ROLE_USER)
                .build();
        user = userRepository.save(user);
        cartRepository.save(Cart.builder().user(user).build());
        log.info("Seeded user -> user@shop.com / User@1234");
    }

    private void seedProducts() {
        if (productRepository.count() > 0) return;

        productRepository.save(Product.builder()
                .name("Wireless Headphones")
                .description("Premium noise-cancelling headphones with 30hr battery.")
                .price(new BigDecimal("199.99"))
                .stockQuantity(50)
                .category("Electronics")
                .imageUrl("https://example.com/headphones.jpg")
                .build());

        productRepository.save(Product.builder()
                .name("Mechanical Keyboard")
                .description("TKL mechanical keyboard with RGB backlight.")
                .price(new BigDecimal("129.99"))
                .stockQuantity(30)
                .category("Electronics")
                .imageUrl("https://example.com/keyboard.jpg")
                .build());

        productRepository.save(Product.builder()
                .name("Ergonomic Chair")
                .description("Fully adjustable chair with lumbar support.")
                .price(new BigDecimal("449.00"))
                .stockQuantity(15)
                .category("Furniture")
                .imageUrl("https://example.com/chair.jpg")
                .build());

        productRepository.save(Product.builder()
                .name("Water Bottle")
                .description("Double-wall insulated 32oz stainless steel bottle.")
                .price(new BigDecimal("34.95"))
                .stockQuantity(200)
                .category("Sports")
                .imageUrl("https://example.com/bottle.jpg")
                .build());

        log.info("Seeded 4 sample products");
    }
}