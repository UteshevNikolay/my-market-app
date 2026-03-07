package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.TestcontainersConfiguration;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.OrderItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
class CartsControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanup() {
        // Order items must be deleted before orders due to FK constraint,
        // and cart items are independent.
        orderItemRepository.deleteAll()
                .then(orderRepository.deleteAll())
                .then(cartItemRepository.deleteAll())
                .block();
    }

    @Test
    void getCartItems_emptyCart() {
        webTestClient.get().uri("/cart/items")
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
        webTestClient.post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // GET /cart/items should show the item with count=1 and correct total
        webTestClient.get().uri("/cart/items")
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
        webTestClient.post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // Then delete it via POST /cart/items with action=DELETE
        webTestClient.post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "DELETE"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // GET /cart/items should now show empty cart (no total, no items)
        webTestClient.get().uri("/cart/items")
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
        webTestClient.post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // POST /buy should redirect to /orders/{id}?newOrder=true
        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", location ->
                        assertThat(location).matches(".*/orders/\\d+\\?newOrder=true"));

        // Cart should now be empty after purchase
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).doesNotContain("Итого:"));
    }
}
