package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.entity.CartItem;
import com.my.project.mymarketapp.entity.Order;
import com.my.project.mymarketapp.entity.OrderItem;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartsService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final ItemMapper itemMapper;

    public CartsService(CartItemRepository cartItemRepository,
                        OrderRepository orderRepository,
                        ItemMapper itemMapper) {
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.itemMapper = itemMapper;
    }

    public List<ItemDto> getCartItems() {
        List<CartItem> cartItems = cartItemRepository.findAll();
        List<ItemDto> result = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            result.add(itemMapper.toDto(cartItem.getItem(), cartItem.getCount()));
        }
        return result;
    }

    @Transactional
    public void updateCartItem(Long id, String action) {
        Optional<CartItem> existing = cartItemRepository.findByItemId(id);
        if (existing.isEmpty()) {
            return;
        }
        CartItem cartItem = existing.get();
        if ("PLUS".equals(action)) {
            cartItem.setCount(cartItem.getCount() + 1);
            cartItemRepository.save(cartItem);
        } else if ("MINUS".equals(action)) {
            if (cartItem.getCount() > 1) {
                cartItem.setCount(cartItem.getCount() - 1);
                cartItemRepository.save(cartItem);
            } else {
                cartItemRepository.delete(cartItem);
            }
        } else if ("DELETE".equals(action)) {
            cartItemRepository.delete(cartItem);
        }
    }

    public int getTotal() {
        List<CartItem> cartItems = cartItemRepository.findAll();
        return cartItems.stream()
                .mapToInt(cartItem -> cartItem.getItem().getPrice() * cartItem.getCount())
                .sum();
    }

    @Transactional
    public Long createOrder() {
        List<CartItem> cartItems = cartItemRepository.findAll();

        Order order = new Order();
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(cartItem.getItem());
            orderItem.setCount(cartItem.getCount());
            orderItem.setPrice(cartItem.getItem().getPrice());
            orderItems.add(orderItem);
        }
        order.getItems().addAll(orderItems);

        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);

        return savedOrder.getId();
    }
}
