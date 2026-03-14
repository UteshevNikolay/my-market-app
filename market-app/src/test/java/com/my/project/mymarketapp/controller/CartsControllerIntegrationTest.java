package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.config.TestcontainersConfiguration;
import com.my.project.mymarketapp.entity.Item;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.OrderItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import com.my.project.mymarketapp.security.AppUserDetails;
import com.my.project.mymarketapp.service.ItemCacheService;
import com.my.project.mymarketapp.service.PaymentClientService;
import com.my.project.payment.client.model.PaymentResponse;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CartsControllerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PaymentClientService paymentClientService;

    @MockitoBean
    private ItemCacheService itemCacheService;

    private static AppUserDetails testUser() {
        return new AppUserDetails(1L, "user1", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @BeforeEach
    void cleanup() {
        webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();

        // Order items must be deleted before orders due to FK constraint,
        // and cart items are independent.
        orderItemRepository.deleteAll()
                .then(orderRepository.deleteAll())
                .then(cartItemRepository.deleteAll())
                .block();

        // Default payment mock: balance always sufficient, payment always succeeds
        PaymentResponse successResponse = new PaymentResponse();
        successResponse.setSuccess(true);
        successResponse.setBalance(90000);

        when(paymentClientService.getBalance()).thenReturn(Mono.just(100000));
        when(paymentClientService.processPayment(anyInt())).thenReturn(Mono.just(successResponse));

        // Cache always misses so integration tests exercise the real DB logic
        when(itemCacheService.getCachedItems(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Mono.empty());
        when(itemCacheService.cacheItems(anyString(), anyString(), anyInt(), anyInt(), anyList()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(4)));
        when(itemCacheService.getCachedItem(anyLong())).thenReturn(Mono.empty());
        when(itemCacheService.cacheItem(anyLong(), any(Item.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(1)));
        when(itemCacheService.getCachedCount(anyString())).thenReturn(Mono.empty());
        when(itemCacheService.cacheCount(anyString(), anyLong()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(1)));
    }

    @Test
    void getCartItems_emptyCart() {
        webTestClient.mutateWith(mockUser(testUser()))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    // Empty cart has no item cards and no "Итого:" total section
                    assertThat(html).doesNotContain("Итого:");
                });
    }

    @Test
    void addAndViewCart() {
        // Add item 1 (Wireless Bluetooth Headphones, price 4990) to cart
        webTestClient.mutateWith(mockUser(testUser())).mutateWith(csrf())
                .post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // GET /cart/items should show the item with count=1 and correct total
        webTestClient.mutateWith(mockUser(testUser()))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Wireless Bluetooth Headphones");
                    // Cart count is rendered in a span as >1<
                    assertThat(html).contains(">1<");
                    // Total is rendered as "Итого: 4990 руб."
                    assertThat(html).contains("Итого: 4990");
                });
    }

    @Test
    void updateCartItem_delete() {
        // First add item 1 to cart
        webTestClient.mutateWith(mockUser(testUser())).mutateWith(csrf())
                .post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // Then delete it via POST /cart/items with action=DELETE
        webTestClient.mutateWith(mockUser(testUser())).mutateWith(csrf())
                .post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "DELETE"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // GET /cart/items should now show empty cart (no total, no items)
        webTestClient.mutateWith(mockUser(testUser()))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).doesNotContain("Wireless Bluetooth Headphones");
                    assertThat(html).doesNotContain("Итого:");
                });
    }

    @Test
    void buy_createsOrder() {
        // Add item 1 to cart
        webTestClient.mutateWith(mockUser(testUser())).mutateWith(csrf())
                .post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // POST /buy should redirect to /orders/{id}?newOrder=true
        webTestClient.mutateWith(mockUser(testUser())).mutateWith(csrf())
                .post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", location ->
                        assertThat(location).matches(".*/orders/\\d+\\?newOrder=true"));

        // Cart should now be empty after purchase
        webTestClient.mutateWith(mockUser(testUser()))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).doesNotContain("Итого:"));
    }
}
