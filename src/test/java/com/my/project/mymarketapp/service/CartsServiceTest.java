package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.entity.CartItem;
import com.my.project.mymarketapp.entity.Item;
import com.my.project.mymarketapp.entity.Order;
import com.my.project.mymarketapp.entity.OrderItem;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartsServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private CartsService cartsService;

    // ---------------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------------

    private Item buildItem(long id, String title, Integer price) {
        Item item = new Item();
        item.setId(id);
        item.setTitle(title);
        item.setDescription("desc-" + id);
        item.setPrice(price);
        item.setImgPath("img-" + id + ".png");
        return item;
    }

    private CartItem buildCartItem(long cartItemId, Item item, int count) {
        CartItem cartItem = new CartItem();
        cartItem.setId(cartItemId);
        cartItem.setItem(item);
        cartItem.setCount(count);
        return cartItem;
    }

    // ---------------------------------------------------------------------------
    // getCartItems tests
    // ---------------------------------------------------------------------------

    @Test
    void getCartItems_returnsMappedItems() {
        Item item1 = buildItem(1L, "Item One", 100);
        Item item2 = buildItem(2L, "Item Two", 200);
        CartItem cartItem1 = buildCartItem(10L, item1, 1);
        CartItem cartItem2 = buildCartItem(20L, item2, 2);

        when(cartItemRepository.findAll()).thenReturn(List.of(cartItem1, cartItem2));

        ItemDto dto1 = new ItemDto(1L, "Item One", "desc-1", 100, "img-1.png", 1);
        ItemDto dto2 = new ItemDto(2L, "Item Two", "desc-2", 200, "img-2.png", 2);
        when(itemMapper.toDto(item1, 1)).thenReturn(dto1);
        when(itemMapper.toDto(item2, 2)).thenReturn(dto2);

        List<ItemDto> result = cartsService.getCartItems();

        verify(itemMapper).toDto(item1, 1);
        verify(itemMapper).toDto(item2, 2);
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(dto1, dto2);
    }

    @Test
    void getCartItems_emptyCart() {
        when(cartItemRepository.findAll()).thenReturn(List.of());

        List<ItemDto> result = cartsService.getCartItems();

        assertThat(result).isEmpty();
        verify(itemMapper, never()).toDto(any(), any());
    }

    // ---------------------------------------------------------------------------
    // updateCartItem tests
    // ---------------------------------------------------------------------------

    @Test
    void updateCartItem_plus() {
        Item item = buildItem(1L, "Widget", 50);
        CartItem cartItem = buildCartItem(10L, item, 2);

        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        cartsService.updateCartItem(1L, "PLUS");

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getCount()).isEqualTo(3);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void updateCartItem_minusDecrement() {
        Item item = buildItem(2L, "Gadget", 75);
        CartItem cartItem = buildCartItem(20L, item, 3);

        when(cartItemRepository.findByItemId(2L)).thenReturn(Optional.of(cartItem));

        cartsService.updateCartItem(2L, "MINUS");

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getCount()).isEqualTo(2);
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void updateCartItem_minusDelete() {
        Item item = buildItem(3L, "Trinket", 10);
        CartItem cartItem = buildCartItem(30L, item, 1);

        when(cartItemRepository.findByItemId(3L)).thenReturn(Optional.of(cartItem));

        cartsService.updateCartItem(3L, "MINUS");

        verify(cartItemRepository).delete(cartItem);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateCartItem_delete() {
        Item item = buildItem(4L, "Doohickey", 20);
        CartItem cartItem = buildCartItem(40L, item, 5);

        when(cartItemRepository.findByItemId(4L)).thenReturn(Optional.of(cartItem));

        cartsService.updateCartItem(4L, "DELETE");

        verify(cartItemRepository).delete(cartItem);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateCartItem_notFound() {
        when(cartItemRepository.findByItemId(99L)).thenReturn(Optional.empty());

        cartsService.updateCartItem(99L, "PLUS");

        verify(cartItemRepository, never()).save(any());
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    // ---------------------------------------------------------------------------
    // getTotal tests
    // ---------------------------------------------------------------------------

    @Test
    void getTotal_calculatesSum() {
        Item item1 = buildItem(1L, "A", 100);
        Item item2 = buildItem(2L, "B", 50);
        CartItem cartItem1 = buildCartItem(10L, item1, 2);
        CartItem cartItem2 = buildCartItem(20L, item2, 3);

        when(cartItemRepository.findAll()).thenReturn(List.of(cartItem1, cartItem2));

        int total = cartsService.getTotal();

        // 100 * 2 + 50 * 3 = 200 + 150 = 350
        assertThat(total).isEqualTo(350);
    }

    @Test
    void getTotal_emptyCart() {
        when(cartItemRepository.findAll()).thenReturn(List.of());

        int total = cartsService.getTotal();

        assertThat(total).isEqualTo(0);
    }

    // ---------------------------------------------------------------------------
    // createOrder tests
    // ---------------------------------------------------------------------------

    @Test
    void createOrder_createsOrderAndClearsCart() {
        Item item1 = buildItem(1L, "Alpha", 100);
        Item item2 = buildItem(2L, "Beta", 200);
        CartItem cartItem1 = buildCartItem(10L, item1, 2);
        CartItem cartItem2 = buildCartItem(20L, item2, 3);
        List<CartItem> cartItems = List.of(cartItem1, cartItem2);

        when(cartItemRepository.findAll()).thenReturn(cartItems);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(42L);
            return o;
        });

        Long returnedId = cartsService.createOrder();

        // Verify the returned id is 42
        assertThat(returnedId).isEqualTo(42L);

        // Capture the Order passed to orderRepository.save
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        // Verify the order has 2 OrderItems with correct prices and counts
        List<OrderItem> orderItems = savedOrder.getItems();
        assertThat(orderItems).hasSize(2);

        OrderItem orderItem1 = orderItems.stream()
                .filter(oi -> oi.getItem().equals(item1))
                .findFirst()
                .orElseThrow();
        assertThat(orderItem1.getCount()).isEqualTo(2);
        assertThat(orderItem1.getPrice()).isEqualTo(100);
        assertThat(orderItem1.getOrder()).isSameAs(savedOrder);

        OrderItem orderItem2 = orderItems.stream()
                .filter(oi -> oi.getItem().equals(item2))
                .findFirst()
                .orElseThrow();
        assertThat(orderItem2.getCount()).isEqualTo(3);
        assertThat(orderItem2.getPrice()).isEqualTo(200);
        assertThat(orderItem2.getOrder()).isSameAs(savedOrder);

        // Verify cart is cleared
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartItem>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(cartItemRepository).deleteAll(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue()).containsExactlyInAnyOrderElementsOf(cartItems);
    }
}
