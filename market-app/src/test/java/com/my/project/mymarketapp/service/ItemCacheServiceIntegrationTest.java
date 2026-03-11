package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.config.RedisTestcontainersConfiguration;
import com.my.project.mymarketapp.config.TestcontainersConfiguration;
import com.my.project.mymarketapp.entity.Item;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RedisTestcontainersConfiguration.class})
class ItemCacheServiceIntegrationTest {

    @Autowired
    private ItemCacheService itemCacheService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @MockitoBean
    private PaymentClientService paymentClientService;

    @BeforeEach
    void clearRedis() {
        reactiveRedisTemplate.getConnectionFactory().getReactiveConnection()
                .serverCommands().flushAll().block();
    }

    @Test
    void cacheItem_thenGetCachedItem_returnsFromCache() {
        Item item = createItem(1L, "Test Phone", "A phone", 50000, "phone.svg");

        // Cache the item
        StepVerifier.create(itemCacheService.cacheItem(1L, item))
                .assertNext(cached -> assertThat(cached.getTitle()).isEqualTo("Test Phone"))
                .verifyComplete();

        // Retrieve from cache
        StepVerifier.create(itemCacheService.getCachedItem(1L))
                .assertNext(cached -> {
                    assertThat(cached.getId()).isEqualTo(1L);
                    assertThat(cached.getTitle()).isEqualTo("Test Phone");
                    assertThat(cached.getDescription()).isEqualTo("A phone");
                    assertThat(cached.getPrice()).isEqualTo(50000);
                    assertThat(cached.getImgPath()).isEqualTo("phone.svg");
                })
                .verifyComplete();
    }

    @Test
    void getCachedItem_miss_returnsEmpty() {
        StepVerifier.create(itemCacheService.getCachedItem(999L))
                .verifyComplete();
    }

    @Test
    void cacheItems_thenGetCachedItems_returnsFromCache() {
        List<Item> items = List.of(
                createItem(1L, "Phone", "A phone", 50000, "phone.svg"),
                createItem(2L, "Laptop", "A laptop", 100000, "laptop.svg")
        );

        // Cache the list
        StepVerifier.create(itemCacheService.cacheItems("", "NO", 10, 1, items))
                .assertNext(cached -> assertThat(cached).hasSize(2))
                .verifyComplete();

        // Retrieve from cache
        StepVerifier.create(itemCacheService.getCachedItems("", "NO", 10, 1))
                .assertNext(cached -> {
                    assertThat(cached).hasSize(2);
                    assertThat(cached.get(0).getTitle()).isEqualTo("Phone");
                    assertThat(cached.get(1).getTitle()).isEqualTo("Laptop");
                })
                .verifyComplete();
    }

    @Test
    void getCachedItems_differentParams_returnsEmpty() {
        List<Item> items = List.of(createItem(1L, "Phone", "A phone", 50000, "phone.svg"));

        StepVerifier.create(itemCacheService.cacheItems("phone", "PRICE", 10, 1, items))
                .assertNext(cached -> assertThat(cached).hasSize(1))
                .verifyComplete();

        // Different search params → cache miss
        StepVerifier.create(itemCacheService.getCachedItems("laptop", "PRICE", 10, 1))
                .verifyComplete();
    }

    @Test
    void cacheCount_thenGetCachedCount_returnsFromCache() {
        StepVerifier.create(itemCacheService.cacheCount("phone", 5L))
                .expectNext(5L)
                .verifyComplete();

        StepVerifier.create(itemCacheService.getCachedCount("phone"))
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    void getCachedCount_miss_returnsEmpty() {
        StepVerifier.create(itemCacheService.getCachedCount("nonexistent"))
                .verifyComplete();
    }

    private Item createItem(Long id, String title, String description, Integer price,
                            String imgPath) {
        Item item = new Item();
        item.setId(id);
        item.setTitle(title);
        item.setDescription(description);
        item.setPrice(price);
        item.setImgPath(imgPath);
        return item;
    }
}
