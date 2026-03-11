package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.entity.CartItem;
import com.my.project.mymarketapp.entity.Item;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.ItemRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemsServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ItemCacheService itemCacheService;

    @InjectMocks
    private ItemsService itemsService;

    @BeforeEach
    void setUpCacheMocks() {
        // Default: cache always misses so existing tests continue to test DB logic
        when(itemCacheService.getCachedItems(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Mono.empty());
        when(itemCacheService.cacheItems(anyString(), anyString(), anyInt(), anyInt(), anyList()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(4)));
        when(itemCacheService.getCachedItem(anyLong())).thenReturn(Mono.empty());
        when(itemCacheService.cacheItem(anyLong(), any(Item.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(1)));
        when(itemCacheService.getCachedCount(anyString())).thenReturn(Mono.empty());
        when(itemCacheService.cacheCount(anyString(), anyLong()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(1)));
    }

    @Test
    void getItems_returnsFlatList() {
        List<Item> items = List.of(
                buildItem(1L, "A", 10),
                buildItem(2L, "B", 20),
                buildItem(3L, "C", 30),
                buildItem(4L, "D", 40),
                buildItem(5L, "E", 50)
        );

        when(itemRepository.findByTitleContainingIgnoreCase(any(), any(Pageable.class)))
                .thenReturn(Flux.fromIterable(items));
        when(cartItemRepository.findByItemId(anyLong())).thenReturn(Mono.empty());

        for (Item item : items) {
            when(itemMapper.toDto(eq(item), eq(0))).thenReturn(buildItemDto(item.getId()));
        }

        StepVerifier.create(itemsService.getItems("", "NO", 8, 1).collectList())
                .assertNext(result -> {
                    assertThat(result).hasSize(5);
                    assertThat(result).noneMatch(dto -> dto.id() == -1L);
                })
                .verifyComplete();
    }

    @Test
    void getItems_withSearch() {
        when(itemRepository.findByTitleContainingIgnoreCase(eq("test"), any(Pageable.class)))
                .thenReturn(Flux.empty());

        StepVerifier.create(itemsService.getItems("test", "NO", 8, 1))
                .verifyComplete();

        verify(itemRepository).findByTitleContainingIgnoreCase(eq("test"), any(Pageable.class));
    }

    @Test
    void getItems_withSortAlpha() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(itemRepository.findByTitleContainingIgnoreCase(any(), any(Pageable.class)))
                .thenReturn(Flux.empty());

        StepVerifier.create(itemsService.getItems("", "ALPHA", 8, 1))
                .verifyComplete();

        verify(itemRepository).findByTitleContainingIgnoreCase(any(), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort()).isEqualTo(Sort.by("title"));
    }

    @Test
    void getItems_withSortPrice() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(itemRepository.findByTitleContainingIgnoreCase(any(), any(Pageable.class)))
                .thenReturn(Flux.empty());

        StepVerifier.create(itemsService.getItems("", "PRICE", 8, 1))
                .verifyComplete();

        verify(itemRepository).findByTitleContainingIgnoreCase(any(), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort()).isEqualTo(Sort.by("price"));
    }

    @Test
    void getItemById_found() {
        Item item = buildItem(10L, "Widget", 99);

        CartItem cartItem = new CartItem();
        cartItem.setItemId(item.getId());
        cartItem.setCount(3);

        when(itemRepository.findById(10L)).thenReturn(Mono.just(item));
        when(cartItemRepository.findByItemId(10L)).thenReturn(Mono.just(cartItem));

        ItemDto expected = new ItemDto(10L, "Widget", "desc-10", 99, "img-10.png", 3);
        when(itemMapper.toDto(item, 3)).thenReturn(expected);

        StepVerifier.create(itemsService.getItemById(10L))
                .assertNext(result -> {
                    verify(itemMapper).toDto(item, 3);
                    assertThat(result).isEqualTo(expected);
                })
                .verifyComplete();
    }

    @Test
    void getItemById_notFound() {
        when(itemRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(itemsService.getItemById(99L))
                .assertNext(result -> {
                    assertThat(result).isEqualTo(ItemDto.empty());
                    verify(itemMapper, never()).toDto(any(), any());
                })
                .verifyComplete();
    }

    @Test
    void updateItemCount_plusNewItem() {
        Item item = buildItem(1L, "NewItem", 50);

        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.empty());
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));

        CartItem savedCartItem = new CartItem();
        savedCartItem.setItemId(1L);
        savedCartItem.setCount(1);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(savedCartItem));

        StepVerifier.create(itemsService.updateItemCount(1L, "PLUS"))
                .verifyComplete();

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(cartItemCaptor.capture());

        CartItem saved = cartItemCaptor.getValue();
        assertThat(saved.getCount()).isEqualTo(1);
        assertThat(saved.getItemId()).isEqualTo(1L);
    }

    @Test
    void updateItemCount_plusExisting() {
        CartItem existing = new CartItem();
        existing.setItemId(2L);
        existing.setCount(2);

        CartItem updated = new CartItem();
        updated.setItemId(2L);
        updated.setCount(3);

        when(cartItemRepository.findByItemId(2L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(itemsService.updateItemCount(2L, "PLUS"))
                .verifyComplete();

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(cartItemCaptor.capture());
        assertThat(cartItemCaptor.getValue().getCount()).isEqualTo(3);
    }

    @Test
    void updateItemCount_minusDecrement() {
        CartItem existing = new CartItem();
        existing.setItemId(3L);
        existing.setCount(3);

        CartItem updated = new CartItem();
        updated.setItemId(3L);
        updated.setCount(2);

        when(cartItemRepository.findByItemId(3L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(itemsService.updateItemCount(3L, "MINUS"))
                .verifyComplete();

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(cartItemCaptor.capture());
        verify(cartItemRepository, never()).delete(any());
        assertThat(cartItemCaptor.getValue().getCount()).isEqualTo(2);
    }

    @Test
    void updateItemCount_minusDelete() {
        CartItem existing = new CartItem();
        existing.setItemId(4L);
        existing.setCount(1);

        when(cartItemRepository.findByItemId(4L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.delete(existing)).thenReturn(Mono.empty());

        StepVerifier.create(itemsService.updateItemCount(4L, "MINUS"))
                .verifyComplete();

        verify(cartItemRepository).delete(existing);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void getPaging_returnsPagingDto() {
        // pageNumber=2, pageSize=8: 2 pages already seen → hasPrevious=true
        // total=10 items: (2-1+1)*8 = 16 > 10 → hasNext=false
        when(itemRepository.countByTitleContainingIgnoreCase(any()))
                .thenReturn(Mono.just(10L));

        StepVerifier.create(itemsService.getPaging("query", 8, 2))
                .assertNext(result -> {
                    assertThat(result.pageNumber()).isEqualTo(2);
                    assertThat(result.pageSize()).isEqualTo(8);
                    assertThat(result.hasPrevious()).isTrue();
                    assertThat(result.hasNext()).isFalse();
                })
                .verifyComplete();
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

    private ItemDto buildItemDto(long id) {
        return new ItemDto(id, "title-" + id, "desc-" + id, 100, "img-" + id + ".png", 0);
    }
}
