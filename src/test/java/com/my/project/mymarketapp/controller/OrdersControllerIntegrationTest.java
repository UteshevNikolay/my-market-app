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
class OrdersControllerIntegrationTest {

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
        // Order items must be deleted before orders due to FK constraint.
        orderItemRepository.deleteAll()
                .then(orderRepository.deleteAll())
                .then(cartItemRepository.deleteAll())
                .block();
    }

    @Test
    void getOrders_empty() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Витрина магазина");
                    // No order cards present in empty state
                    assertThat(html).doesNotContain("Заказ №");
                });
    }

    @Test
    void getOrders_afterPurchase() {
        addItemAndBuy(1L);

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Заказ №");
                    assertThat(html).contains("Wireless Bluetooth Headphones");
                });
    }

    @Test
    void getOrder_returnsOrderDetail() {
        Long orderId = addItemAndBuy(1L);

        // GET /orders/{id} without ?newOrder param (defaults to false)
        webTestClient.get().uri("/orders/{id}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Заказ №" + orderId);
                    // Item title appears in the order detail
                    assertThat(html).contains("Wireless Bluetooth Headphones");
                    // Total sum section rendered as "Сумма: XXXX руб."
                    assertThat(html).contains("Сумма:");
                    // No success banner (newOrder=false by default)
                    assertThat(html).doesNotContain("Поздравляем");
                });
    }

    private Long addItemAndBuy(long itemId) {
        webTestClient.post().uri("/items")
                .body(BodyInserters.fromFormData("id", String.valueOf(itemId))
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        String location = webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(String.class)
                .getResponseHeaders()
                .getFirst("Location");

        assertThat(location).isNotNull();

        // Location header is like: /orders/42?newOrder=true
        String path = location.contains("?") ? location.substring(0, location.indexOf('?')) : location;
        String idStr = path.substring(path.lastIndexOf('/') + 1);
        return Long.parseLong(idStr);
    }
}
