package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.entity.CartItem;
import com.my.project.mymarketapp.entity.Item;
import com.my.project.mymarketapp.entity.Order;
import com.my.project.mymarketapp.entity.OrderItem;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.ItemRepository;
import com.my.project.mymarketapp.repository.OrderItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartsServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private CartsService cartsService;

    @Test
    void getCartItems_returnsMappedItems() {
        Item item1 = buildItem(1L, "Item One", 100);
        Item item2 = buildItem(2L, "Item Two", 200);
        CartItem cartItem1 = buildCartItem(10L, 1L, 1);
        CartItem cartItem2 = buildCartItem(20L, 2L, 2);

        when(cartItemRepository.findAll()).thenReturn(Flux.fromIterable(List.of(cartItem1, cartItem2)));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(item2));

        ItemDto dto1 = new ItemDto(1L, "Item One", "desc-1", 100, "img-1.png", 1);
        ItemDto dto2 = new ItemDto(2L, "Item Two", "desc-2", 200, "img-2.png", 2);
        when(itemMapper.toDto(item1, 1)).thenReturn(dto1);
        when(itemMapper.toDto(item2, 2)).thenReturn(dto2);

        StepVerifier.create(cartsService.getCartItems().collectList())
                .assertNext(result -> {
                    assertThat(result).hasSize(2);
                    assertThat(result).containsExactly(dto1, dto2);
                    verify(itemMapper).toDto(item1, 1);
                    verify(itemMapper).toDto(item2, 2);
                })
                .verifyComplete();
    }

    @Test
    void getCartItems_emptyCart() {
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(cartsService.getCartItems().collectList())
                .assertNext(result -> {
                    assertThat(result).isEmpty();
                    verify(itemMapper, never()).toDto(any(), any());
                })
                .verifyComplete();
    }

    @Test
    void updateCartItem_plus() {
        CartItem cartItem = buildCartItem(10L, 1L, 2);
        CartItem updated = buildCartItem(10L, 1L, 3);

        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(cartsService.updateCartItem(1L, "PLUS"))
                .verifyComplete();

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getCount()).isEqualTo(3);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void updateCartItem_minusDecrement() {
        CartItem cartItem = buildCartItem(20L, 2L, 3);
        CartItem updated = buildCartItem(20L, 2L, 2);

        when(cartItemRepository.findByItemId(2L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(cartsService.updateCartItem(2L, "MINUS"))
                .verifyComplete();

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getCount()).isEqualTo(2);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void updateCartItem_minusDelete() {
        CartItem cartItem = buildCartItem(30L, 3L, 1);

        when(cartItemRepository.findByItemId(3L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

        StepVerifier.create(cartsService.updateCartItem(3L, "MINUS"))
                .verifyComplete();

        verify(cartItemRepository).delete(cartItem);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateCartItem_delete() {
        CartItem cartItem = buildCartItem(40L, 4L, 5);

        when(cartItemRepository.findByItemId(4L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

        StepVerifier.create(cartsService.updateCartItem(4L, "DELETE"))
                .verifyComplete();

        verify(cartItemRepository).delete(cartItem);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateCartItem_notFound() {
        when(cartItemRepository.findByItemId(99L)).thenReturn(Mono.empty());

        StepVerifier.create(cartsService.updateCartItem(99L, "PLUS"))
                .verifyComplete();

        verify(cartItemRepository, never()).save(any());
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void getTotal_calculatesSum() {
        Item item1 = buildItem(1L, "A", 100);
        Item item2 = buildItem(2L, "B", 50);
        CartItem cartItem1 = buildCartItem(10L, 1L, 2);
        CartItem cartItem2 = buildCartItem(20L, 2L, 3);

        when(cartItemRepository.findAll()).thenReturn(Flux.fromIterable(List.of(cartItem1, cartItem2)));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(item2));

        // 100 * 2 + 50 * 3 = 200 + 150 = 350
        StepVerifier.create(cartsService.getTotal())
                .assertNext(total -> assertThat(total).isEqualTo(350))
                .verifyComplete();
    }

    @Test
    void getTotal_emptyCart() {
        when(cartItemRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(cartsService.getTotal())
                .assertNext(total -> assertThat(total).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    void createOrder_createsOrderAndClearsCart() {
        Item item1 = buildItem(1L, "Alpha", 100);
        Item item2 = buildItem(2L, "Beta", 200);
        CartItem cartItem1 = buildCartItem(10L, 1L, 2);
        CartItem cartItem2 = buildCartItem(20L, 2L, 3);
        List<CartItem> cartItems = List.of(cartItem1, cartItem2);

        Order savedOrder = new Order();
        savedOrder.setId(42L);

        OrderItem oi1 = new OrderItem();
        oi1.setOrderId(42L);
        oi1.setItemId(1L);
        oi1.setCount(2);
        oi1.setPrice(100);

        OrderItem oi2 = new OrderItem();
        oi2.setOrderId(42L);
        oi2.setItemId(2L);
        oi2.setCount(3);
        oi2.setPrice(200);

        when(cartItemRepository.findAll()).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(item2));
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(List.of(oi1, oi2)));
        when(cartItemRepository.deleteAll(cartItems)).thenReturn(Mono.empty());

        StepVerifier.create(cartsService.createOrder())
                .assertNext(returnedId -> assertThat(returnedId).isEqualTo(42L))
                .verifyComplete();

        // Verify order was saved
        verify(orderRepository).save(any(Order.class));

        // Verify cart was cleared with the original cart items
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartItem>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(cartItemRepository).deleteAll(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue()).containsExactlyInAnyOrderElementsOf(cartItems);

        // Verify order items were saved with correct data
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
        List<OrderItem> savedOrderItems = orderItemsCaptor.getValue();
        assertThat(savedOrderItems).hasSize(2);

        OrderItem capturedOi1 = savedOrderItems.stream()
                .filter(oi -> oi.getItemId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(capturedOi1.getOrderId()).isEqualTo(42L);
        assertThat(capturedOi1.getCount()).isEqualTo(2);
        assertThat(capturedOi1.getPrice()).isEqualTo(100);

        OrderItem capturedOi2 = savedOrderItems.stream()
                .filter(oi -> oi.getItemId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertThat(capturedOi2.getOrderId()).isEqualTo(42L);
        assertThat(capturedOi2.getCount()).isEqualTo(3);
        assertThat(capturedOi2.getPrice()).isEqualTo(200);
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

    private CartItem buildCartItem(long cartItemId, long itemId, int count) {
        CartItem cartItem = new CartItem();
        cartItem.setId(cartItemId);
        cartItem.setItemId(itemId);
        cartItem.setCount(count);
        return cartItem;
    }
}
