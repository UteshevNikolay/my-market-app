package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.dto.PagingDto;
import com.my.project.mymarketapp.entity.CartItem;
import com.my.project.mymarketapp.entity.Item;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemsServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemsService itemsService;

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

    private ItemDto buildItemDto(long id) {
        return new ItemDto(id, "title-" + id, "desc-" + id, 100, "img-" + id + ".png", 0);
    }

    // ---------------------------------------------------------------------------
    // getItems tests
    // ---------------------------------------------------------------------------

    @Test
    void getItems_returnsChunkedRows() {
        // Arrange: 5 items so we expect 2 rows (4 + 1 padded to 4)
        List<Item> items = List.of(
                buildItem(1L, "A", 10),
                buildItem(2L, "B", 20),
                buildItem(3L, "C", 30),
                buildItem(4L, "D", 40),
                buildItem(5L, "E", 50)
        );

        @SuppressWarnings("unchecked")
        Page<Item> page = mock(Page.class);
        when(page.getContent()).thenReturn(items);
        when(itemRepository.findByTitleContainingIgnoreCase(any(), any(Pageable.class))).thenReturn(page);
        when(cartItemRepository.findByItemId(any())).thenReturn(Optional.empty());

        for (Item item : items) {
            when(itemMapper.toDto(eq(item), eq(0))).thenReturn(buildItemDto(item.getId()));
        }

        // Act
        List<List<ItemDto>> rows = itemsService.getItems("", "NO", 8, 1);

        // Assert: two rows
        assertThat(rows).hasSize(2);

        // First row: 4 real items
        assertThat(rows.get(0)).hasSize(4);
        assertThat(rows.get(0)).noneMatch(dto -> dto.id() == -1L);

        // Second row: 1 real item + 3 empty padding items
        assertThat(rows.get(1)).hasSize(4);
        assertThat(rows.get(1).get(0).id()).isNotEqualTo(-1L);
        assertThat(rows.get(1).get(1)).isEqualTo(ItemDto.empty());
        assertThat(rows.get(1).get(2)).isEqualTo(ItemDto.empty());
        assertThat(rows.get(1).get(3)).isEqualTo(ItemDto.empty());
    }

    @Test
    void getItems_withSearch() {
        @SuppressWarnings("unchecked")
        Page<Item> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of());
        when(itemRepository.findByTitleContainingIgnoreCase(eq("test"), any(Pageable.class))).thenReturn(page);

        itemsService.getItems("test", "NO", 8, 1);

        verify(itemRepository).findByTitleContainingIgnoreCase(eq("test"), any(Pageable.class));
    }

    @Test
    void getItems_withSortAlpha() {
        @SuppressWarnings("unchecked")
        Page<Item> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of());
        when(itemRepository.findByTitleContainingIgnoreCase(any(), any(Pageable.class))).thenReturn(page);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        itemsService.getItems("", "ALPHA", 8, 1);

        verify(itemRepository).findByTitleContainingIgnoreCase(any(), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort()).isEqualTo(Sort.by("title"));
    }

    @Test
    void getItems_withSortPrice() {
        @SuppressWarnings("unchecked")
        Page<Item> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of());
        when(itemRepository.findByTitleContainingIgnoreCase(any(), any(Pageable.class))).thenReturn(page);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        itemsService.getItems("", "PRICE", 8, 1);

        verify(itemRepository).findByTitleContainingIgnoreCase(any(), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort()).isEqualTo(Sort.by("price"));
    }

    // ---------------------------------------------------------------------------
    // getItemById tests
    // ---------------------------------------------------------------------------

    @Test
    void getItemById_found() {
        Item item = buildItem(10L, "Widget", 99);
        CartItem cartItem = new CartItem();
        cartItem.setItem(item);
        cartItem.setCount(3);

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByItemId(10L)).thenReturn(Optional.of(cartItem));

        ItemDto expected = new ItemDto(10L, "Widget", "desc-10", 99, "img-10.png", 3);
        when(itemMapper.toDto(item, 3)).thenReturn(expected);

        ItemDto result = itemsService.getItemById(10L);

        verify(itemMapper).toDto(item, 3);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getItemById_notFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        ItemDto result = itemsService.getItemById(99L);

        assertThat(result).isEqualTo(ItemDto.empty());
        verify(itemMapper, never()).toDto(any(), any());
    }

    // ---------------------------------------------------------------------------
    // updateItemCount tests
    // ---------------------------------------------------------------------------

    @Test
    void updateItemCount_plusNewItem() {
        Item item = buildItem(1L, "NewItem", 50);

        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        itemsService.updateItemCount(1L, "PLUS");

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(cartItemCaptor.capture());

        CartItem saved = cartItemCaptor.getValue();
        assertThat(saved.getCount()).isEqualTo(1);
        assertThat(saved.getItem()).isEqualTo(item);
    }

    @Test
    void updateItemCount_plusExisting() {
        Item item = buildItem(2L, "ExistingItem", 75);
        CartItem existing = new CartItem();
        existing.setItem(item);
        existing.setCount(2);

        when(cartItemRepository.findByItemId(2L)).thenReturn(Optional.of(existing));

        itemsService.updateItemCount(2L, "PLUS");

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(cartItemCaptor.capture());
        assertThat(cartItemCaptor.getValue().getCount()).isEqualTo(3);
    }

    @Test
    void updateItemCount_minusDecrement() {
        Item item = buildItem(3L, "DecrItem", 30);
        CartItem existing = new CartItem();
        existing.setItem(item);
        existing.setCount(3);

        when(cartItemRepository.findByItemId(3L)).thenReturn(Optional.of(existing));

        itemsService.updateItemCount(3L, "MINUS");

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(cartItemCaptor.capture());
        verify(cartItemRepository, never()).delete(any());
        assertThat(cartItemCaptor.getValue().getCount()).isEqualTo(2);
    }

    @Test
    void updateItemCount_minusDelete() {
        Item item = buildItem(4L, "DelItem", 20);
        CartItem existing = new CartItem();
        existing.setItem(item);
        existing.setCount(1);

        when(cartItemRepository.findByItemId(4L)).thenReturn(Optional.of(existing));

        itemsService.updateItemCount(4L, "MINUS");

        verify(cartItemRepository).delete(existing);
        verify(cartItemRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------------
    // getPaging tests
    // ---------------------------------------------------------------------------

    @Test
    void getPaging_returnsPagingDto() {
        @SuppressWarnings("unchecked")
        Page<Item> page = mock(Page.class);
        when(page.hasPrevious()).thenReturn(true);
        when(page.hasNext()).thenReturn(false);
        when(itemRepository.findByTitleContainingIgnoreCase(any(), any(Pageable.class))).thenReturn(page);

        PagingDto result = itemsService.getPaging("query", 8, 2);

        // pageNumber must remain 1-based as supplied to the method
        assertThat(result.pageNumber()).isEqualTo(2);
        assertThat(result.pageSize()).isEqualTo(8);
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isFalse();
    }
}
