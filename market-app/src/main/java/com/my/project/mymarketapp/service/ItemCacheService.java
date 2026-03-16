package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.entity.Item;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ItemCacheService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;
    private final Duration ttl;

    public ItemCacheService(ReactiveRedisTemplate<String, String> redisTemplate,
                            JsonMapper jsonMapper,
                            @Value("${cache.item.ttl-minutes:2}") int ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public Mono<Item> getCachedItem(Long id) {
        String key = "item:" + id;
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(jsonMapper.readValue(json, Item.class));
                    } catch (JacksonException e) {
                        return Mono.empty();
                    }
                });
    }

    public Mono<Item> cacheItem(Long id, Item item) {
        String key = "item:" + id;
        try {
            String json = jsonMapper.writeValueAsString(item);
            return redisTemplate.opsForValue().set(key, json, ttl)
                    .thenReturn(item);
        } catch (JacksonException e) {
            return Mono.just(item);
        }
    }

    public Mono<List<Item>> getCachedItems(String search, String sort, int pageSize,
                                           int pageNumber) {
        String key = buildItemsKey(search, sort, pageSize, pageNumber);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        List<Item> items = jsonMapper.readValue(json,
                                new TypeReference<List<Item>>() {
                                });
                        return Mono.just(items);
                    } catch (JacksonException e) {
                        return Mono.empty();
                    }
                });
    }

    public Mono<List<Item>> cacheItems(String search, String sort, int pageSize, int pageNumber,
                                       List<Item> items) {
        String key = buildItemsKey(search, sort, pageSize, pageNumber);
        try {
            String json = jsonMapper.writeValueAsString(items);
            return redisTemplate.opsForValue().set(key, json, ttl)
                    .thenReturn(items);
        } catch (JacksonException e) {
            return Mono.just(items);
        }
    }

    public Mono<Long> getCachedCount(String search) {
        String key = String.format("items:count:%s", search != null ? search : "");
        return redisTemplate.opsForValue().get(key)
                .map(Long::parseLong);
    }

    public Mono<Long> cacheCount(String search, Long count) {
        String key = String.format("items:count:%s", search != null ? search : "");
        return redisTemplate.opsForValue().set(key, String.valueOf(count), ttl)
                .thenReturn(count);
    }

    private String buildItemsKey(String search, String sort, int pageSize, int pageNumber) {
        return String.format("items:search:%s:sort:%s:size:%d:page:%d",
                search != null ? search : "",
                sort != null ? sort : "NO",
                pageSize,
                pageNumber);
    }
}
