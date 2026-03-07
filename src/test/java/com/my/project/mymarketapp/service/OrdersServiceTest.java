package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.dto.OrderDto;
import com.my.project.mymarketapp.entity.Order;
import com.my.project.mymarketapp.mapper.OrderMapper;
import com.my.project.mymarketapp.repository.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrdersService ordersService;

    @Test
    void getAllOrders_returnsMappedOrders() {
        Order order1 = buildOrder(1L);
        Order order2 = buildOrder(2L);
        OrderDto dto1 = buildOrderDto(1L);
        OrderDto dto2 = buildOrderDto(2L);

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));
        when(orderMapper.toDto(order1)).thenReturn(dto1);
        when(orderMapper.toDto(order2)).thenReturn(dto2);

        List<OrderDto> result = ordersService.getAllOrders();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(dto1, dto2);
    }

    @Test
    void getAllOrders_empty() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<OrderDto> result = ordersService.getAllOrders();

        assertThat(result).isEmpty();
    }

    @Test
    void getOrderById_found() {
        Order order = buildOrder(10L);
        OrderDto expected = buildOrderDto(10L);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(expected);

        OrderDto result = ordersService.getOrderById(10L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getOrderById_notFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordersService.getOrderById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    private Order buildOrder(long id) {
        Order order = new Order();
        order.setId(id);
        return order;
    }

    private OrderDto buildOrderDto(long id) {
        return new OrderDto(id, List.of(new ItemDto(1L, "title-" + id, "desc-" + id, 100,
                "img-" + id + ".png", 1)), 100);
    }

}
