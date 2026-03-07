package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.TestcontainersConfiguration;
import com.my.project.mymarketapp.repository.CartItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
class ItemsControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CartItemRepository cartItemRepository;

    @AfterEach
    void cleanup() {
        cartItemRepository.deleteAll().block();
    }

    @Test
    void getItems_returnsItemsPage() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Витрина магазина");
                    assertThat(html).contains("Wireless Bluetooth Headphones");
                });
    }

    @Test
    void getItems_withSearch() {
        webTestClient.get().uri("/items?search=Keyboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Keyboard");
                    // Items not matching "Keyboard" should not appear
                    assertThat(html).doesNotContain("Stainless Steel Kettle");
                    assertThat(html).doesNotContain("Smart LED Desk Lamp");
                });
    }

    @Test
    void getItems_withPagination() {
        webTestClient.get().uri("/items?pageSize=2&pageNumber=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Страница: 1");
                });
    }

    @Test
    void updateItemCount_addsToCart() {
        // POST /items with action=PLUS should redirect
        webTestClient.post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // GET /items/1 and verify count shows 1 in the cart
        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Wireless Bluetooth Headphones");
                    // The count span renders as ">1<" in the HTML
                    assertThat(html).contains(">1<");
                });
    }

    @Test
    void getItem_returnsItemDetail() {
        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Wireless Bluetooth Headphones"));
    }
}
