package com.my.project.mymarketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.project.mymarketapp.entity.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class ItemCacheService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public ItemCacheService(ReactiveRedisTemplate<String, String> redisTemplate,
                            ObjectMapper objectMapper,
                            @Value("${cache.item.ttl-minutes:2}") int ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public Mono<Item> getCachedItem(Long id) {
        String key = "item:" + id;
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, Item.class));
                    } catch (JsonProcessingException e) {
                        return Mono.empty();
                    }
                });
    }

    public Mono<Item> cacheItem(Long id, Item item) {
        String key = "item:" + id;
        try {
            String json = objectMapper.writeValueAsString(item);
            return redisTemplate.opsForValue().set(key, json, ttl)
                    .thenReturn(item);
        } catch (JsonProcessingException e) {
            return Mono.just(item);
        }
    }

    public Mono<List<Item>> getCachedItems(String search, String sort, int pageSize, int pageNumber) {
        String key = buildItemsKey(search, sort, pageSize, pageNumber);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        List<Item> items = objectMapper.readValue(json, new TypeReference<List<Item>>() {});
                        return Mono.just(items);
                    } catch (JsonProcessingException e) {
                        return Mono.empty();
                    }
                });
    }

    public Mono<List<Item>> cacheItems(String search, String sort, int pageSize, int pageNumber, List<Item> items) {
        String key = buildItemsKey(search, sort, pageSize, pageNumber);
        try {
            String json = objectMapper.writeValueAsString(items);
            return redisTemplate.opsForValue().set(key, json, ttl)
                    .thenReturn(items);
        } catch (JsonProcessingException e) {
            return Mono.just(items);
        }
    }

    public Mono<Long> getCachedCount(String search) {
        String key = "items:count:" + (search != null ? search : "");
        return redisTemplate.opsForValue().get(key)
                .map(Long::parseLong);
    }

    public Mono<Long> cacheCount(String search, Long count) {
        String key = "items:count:" + (search != null ? search : "");
        return redisTemplate.opsForValue().set(key, String.valueOf(count), ttl)
                .thenReturn(count);
    }

    private String buildItemsKey(String search, String sort, int pageSize, int pageNumber) {
        return "items:search:" + (search != null ? search : "")
                + ":sort:" + (sort != null ? sort : "NO")
                + ":size:" + pageSize
                + ":page:" + pageNumber;
    }
}
