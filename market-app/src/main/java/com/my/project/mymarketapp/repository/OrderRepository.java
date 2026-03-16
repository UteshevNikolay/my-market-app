package com.my.project.mymarketapp.repository;

import com.my.project.mymarketapp.entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Flux<Order> findAllByUserId(Long userId);

    Mono<Order> findByIdAndUserId(Long id, Long userId);
}
