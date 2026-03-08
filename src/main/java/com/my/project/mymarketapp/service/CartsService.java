package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.entity.Order;
import com.my.project.mymarketapp.entity.OrderItem;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.ItemRepository;
import com.my.project.mymarketapp.repository.OrderItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CartsService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemMapper itemMapper;

    public CartsService(CartItemRepository cartItemRepository,
                        ItemRepository itemRepository,
                        OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ItemMapper itemMapper) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.itemMapper = itemMapper;
    }

    public Flux<ItemDto> getCartItems() {
        return cartItemRepository.findAll()
                .concatMap(cartItem -> itemRepository.findById(cartItem.getItemId())
                        .map(item -> itemMapper.toDto(item, cartItem.getCount()))
                );
    }

    public Mono<Void> updateCartItem(Long id, String action) {
        return cartItemRepository.findByItemId(id)
                .flatMap(cartItem -> {
                    if ("PLUS".equals(action)) {
                        cartItem.setCount(cartItem.getCount() + 1);
                        return cartItemRepository.save(cartItem).then();
                    } else if ("MINUS".equals(action)) {
                        if (cartItem.getCount() > 1) {
                            cartItem.setCount(cartItem.getCount() - 1);
                            return cartItemRepository.save(cartItem).then();
                        } else {
                            return cartItemRepository.delete(cartItem);
                        }
                    } else if ("DELETE".equals(action)) {
                        return cartItemRepository.delete(cartItem);
                    }
                    return Mono.<Void>empty();
                })
                .then();
    }

    public Mono<Integer> getTotal() {
        return cartItemRepository.findAll()
                .flatMap(cartItem -> itemRepository.findById(cartItem.getItemId())
                        .map(item -> item.getPrice() * cartItem.getCount())
                )
                .reduce(0, Integer::sum);
    }

    @Transactional
    public Mono<Long> createOrder() {
        return cartItemRepository.findAll().collectList()
                .flatMap(cartItems -> {
                    Order order = new Order();
                    return orderRepository.save(order)
                            .flatMap(savedOrder ->
                                    Flux.fromIterable(cartItems)
                                            .concatMap(ci -> itemRepository.findById(ci.getItemId())
                                                    .map(item -> {
                                                        OrderItem oi = new OrderItem();
                                                        oi.setOrderId(savedOrder.getId());
                                                        oi.setItemId(ci.getItemId());
                                                        oi.setCount(ci.getCount());
                                                        oi.setPrice(item.getPrice());
                                                        return oi;
                                                    })
                                            )
                                            .collectList()
                                            .flatMapMany(orderItems -> orderItemRepository.saveAll(orderItems))
                                            .then(cartItemRepository.deleteAll(cartItems))
                                            .thenReturn(savedOrder.getId())
                            );
                });
    }
}
