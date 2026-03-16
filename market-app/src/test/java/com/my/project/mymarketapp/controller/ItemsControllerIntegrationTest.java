package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.config.TestcontainersConfiguration;
import com.my.project.mymarketapp.entity.Item;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.security.AppUserDetails;
import com.my.project.mymarketapp.service.ItemCacheService;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
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
class ItemsControllerIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @Autowired
    private CartItemRepository cartItemRepository;

    @MockitoBean
    private ItemCacheService itemCacheService;

    private static AppUserDetails testUser() {
        return new AppUserDetails(1L, "user1", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @BeforeEach
    void setUpCacheMocks() {
        webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();

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
        webTestClient.mutateWith(mockUser(testUser())).mutateWith(csrf())
                .post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // GET /items/1 and verify count shows 1 in the cart
        webTestClient.mutateWith(mockUser(testUser()))
                .get().uri("/items/1")
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
