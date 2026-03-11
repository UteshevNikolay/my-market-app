package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.OrderDto;
import com.my.project.mymarketapp.entity.Order;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.ItemRepository;
import com.my.project.mymarketapp.repository.OrderItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrdersService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public OrdersService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         ItemRepository itemRepository,
                         ItemMapper itemMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    public Flux<OrderDto> getAllOrders() {
        return orderRepository.findAll()
                .concatMap(this::buildOrderDto);
    }

    public Mono<OrderDto> getOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found: " + id)))
                .flatMap(this::buildOrderDto);
    }

    private Mono<OrderDto> buildOrderDto(Order order) {
        return orderItemRepository.findByOrderId(order.getId())
                .concatMap(oi -> itemRepository.findById(oi.getItemId())
                        .map(item -> itemMapper.orderItemToDto(item, oi))
                )
                .collectList()
                .map(items -> {
                    int totalSum = items.stream().mapToInt(i -> i.price() * i.count()).sum();
                    return new OrderDto(order.getId(), items, totalSum);
                });
    }
}
