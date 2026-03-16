package com.my.project.mymarketapp.repository;

import com.my.project.mymarketapp.entity.CartItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Mono<CartItem> findByUserIdAndItemId(Long userId, Long itemId);

    Flux<CartItem> findAllByUserId(Long userId);

    Mono<Void> deleteAllByUserId(Long userId);
}
