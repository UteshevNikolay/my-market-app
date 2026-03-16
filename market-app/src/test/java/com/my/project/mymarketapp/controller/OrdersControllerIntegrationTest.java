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
class OrdersControllerIntegrationTest {

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

        // Order items must be deleted before orders due to FK constraint.
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
    void getOrders_empty() {
        webTestClient.mutateWith(mockUser(testUser()))
                .get().uri("/orders")
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

        webTestClient.mutateWith(mockUser(testUser()))
                .get().uri("/orders")
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
        webTestClient.mutateWith(mockUser(testUser()))
                .get().uri("/orders/{id}", orderId)
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
        webTestClient.mutateWith(mockUser(testUser())).mutateWith(csrf())
                .post().uri("/items")
                .body(BodyInserters.fromFormData("id", String.valueOf(itemId))
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        String location = webTestClient.mutateWith(mockUser(testUser())).mutateWith(csrf())
                .post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(String.class)
                .getResponseHeaders()
                .getFirst("Location");

        assertThat(location).isNotNull();

        // Location header is like: /orders/42?newOrder=true
        String path = location.contains("?") ? location.substring(0, location.indexOf('?')) :
                location;
        String idStr = path.substring(path.lastIndexOf('/') + 1);
        return Long.parseLong(idStr);
    }
}
