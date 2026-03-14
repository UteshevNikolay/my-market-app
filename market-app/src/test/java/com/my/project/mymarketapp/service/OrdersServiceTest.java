package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.dto.OrderDto;
import com.my.project.mymarketapp.entity.Item;
import com.my.project.mymarketapp.entity.Order;
import com.my.project.mymarketapp.entity.OrderItem;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.ItemRepository;
import com.my.project.mymarketapp.repository.OrderItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private OrdersService ordersService;

    @Test
    void getAllOrders_returnsMappedOrders() {
        Order order1 = buildOrder(1L);
        Order order2 = buildOrder(2L);

        Item item1 = buildItem(10L, "Widget", 100);
        Item item2 = buildItem(20L, "Gadget", 200);

        OrderItem oi1 = buildOrderItem(100L, 1L, 10L, 100, 1);
        OrderItem oi2 = buildOrderItem(200L, 2L, 20L, 200, 2);

        ItemDto dto1 = new ItemDto(10L, "Widget", "desc-10", 100, "img-10.png", 1);
        ItemDto dto2 = new ItemDto(20L, "Gadget", "desc-20", 200, "img-20.png", 2);

        when(orderRepository.findAllByUserId(USER_ID)).thenReturn(Flux.fromIterable(List.of(order1, order2)));

        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(oi1));
        when(itemRepository.findById(10L)).thenReturn(Mono.just(item1));
        when(itemMapper.orderItemToDto(item1, oi1)).thenReturn(dto1);

        when(orderItemRepository.findByOrderId(2L)).thenReturn(Flux.just(oi2));
        when(itemRepository.findById(20L)).thenReturn(Mono.just(item2));
        when(itemMapper.orderItemToDto(item2, oi2)).thenReturn(dto2);

        StepVerifier.create(ordersService.getAllOrders(USER_ID).collectList())
                .assertNext(result -> {
                    assertThat(result).hasSize(2);

                    OrderDto resultDto1 = result.stream()
                            .filter(o -> o.id().equals(1L))
                            .findFirst()
                            .orElseThrow();
                    assertThat(resultDto1.items()).containsExactly(dto1);
                    assertThat(resultDto1.totalSum()).isEqualTo(100);  // 100 * 1

                    OrderDto resultDto2 = result.stream()
                            .filter(o -> o.id().equals(2L))
                            .findFirst()
                            .orElseThrow();
                    assertThat(resultDto2.items()).containsExactly(dto2);
                    assertThat(resultDto2.totalSum()).isEqualTo(400);  // 200 * 2
                })
                .verifyComplete();
    }

    @Test
    void getAllOrders_empty() {
        when(orderRepository.findAllByUserId(USER_ID)).thenReturn(Flux.empty());

        StepVerifier.create(ordersService.getAllOrders(USER_ID).collectList())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
    }

    @Test
    void getOrderById_found() {
        Order order = buildOrder(10L);
        Item item = buildItem(10L, "Widget", 99);
        OrderItem oi = buildOrderItem(1L, 10L, 10L, 99, 1);
        ItemDto expected = new ItemDto(10L, "Widget", "desc-10", 99, "img-10.png", 1);

        when(orderRepository.findByIdAndUserId(anyLong(), eq(USER_ID))).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(Flux.just(oi));
        when(itemRepository.findById(10L)).thenReturn(Mono.just(item));
        when(itemMapper.orderItemToDto(item, oi)).thenReturn(expected);

        StepVerifier.create(ordersService.getOrderById(10L, USER_ID))
                .assertNext(result -> {
                    assertThat(result.id()).isEqualTo(10L);
                    assertThat(result.items()).containsExactly(expected);
                    assertThat(result.totalSum()).isEqualTo(99);  // 99 * 1
                })
                .verifyComplete();
    }

    @Test
    void getOrderById_notFound() {
        when(orderRepository.findByIdAndUserId(anyLong(), eq(USER_ID))).thenReturn(Mono.empty());

        StepVerifier.create(ordersService.getOrderById(99L, USER_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IllegalArgumentException.class);
                    assertThat(error.getMessage()).contains("99");
                })
                .verify();
    }

    private Order buildOrder(long id) {
        Order order = new Order();
        order.setId(id);
        return order;
    }

    private Item buildItem(long id, String title, Integer price) {
        Item item = new Item();
        item.setId(id);
        item.setTitle(title);
        item.setDescription("desc-" + id);
        item.setPrice(price);
        item.setImgPath("img-" + id + ".png");
        return item;
    }

    private OrderItem buildOrderItem(long id, long orderId, long itemId, int price, int count) {
        OrderItem oi = new OrderItem();
        oi.setId(id);
        oi.setOrderId(orderId);
        oi.setItemId(itemId);
        oi.setPrice(price);
        oi.setCount(count);
        return oi;
    }
}
