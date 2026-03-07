package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.OrderDto;
import com.my.project.mymarketapp.mapper.OrderMapper;
import com.my.project.mymarketapp.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrdersService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrdersService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toDto)
                .toList();
    }

    public OrderDto getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }
}
