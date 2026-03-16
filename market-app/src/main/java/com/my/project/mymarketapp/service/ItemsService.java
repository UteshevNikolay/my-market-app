package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.dto.PagingDto;
import com.my.project.mymarketapp.entity.CartItem;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.ItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ItemsService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemMapper itemMapper;
    private final ItemCacheService itemCacheService;

    public ItemsService(ItemRepository itemRepository,
                        CartItemRepository cartItemRepository,
                        ItemMapper itemMapper,
                        ItemCacheService itemCacheService) {
        this.itemRepository = itemRepository;
        this.cartItemRepository = cartItemRepository;
        this.itemMapper = itemMapper;
        this.itemCacheService = itemCacheService;
    }

    public Flux<ItemDto> getItems(String search, String sort, int pageSize, int pageNumber,
                                  Long userId) {
        Pageable pageable = buildPageable(pageSize, pageNumber, sort);

        return itemCacheService.getCachedItems(search, sort, pageSize, pageNumber)
                .flatMapMany(Flux::fromIterable)
                .switchIfEmpty(
                        itemRepository.findByTitleContainingIgnoreCase(search, pageable)
                                .collectList()
                                .flatMap(items -> itemCacheService.cacheItems(search, sort,
                                        pageSize, pageNumber, items))
                                .flatMapMany(Flux::fromIterable)
                )
                .concatMap(item -> {
                    if (userId == null) {
                        return Mono.just(itemMapper.toDto(item, 0));
                    }
                    return cartItemRepository.findByUserIdAndItemId(userId, item.getId())
                            .map(CartItem::getCount)
                            .defaultIfEmpty(0)
                            .map(count -> itemMapper.toDto(item, count));
                });
    }

    public Mono<ItemDto> getItemById(Long id, Long userId) {
        return itemCacheService.getCachedItem(id)
                .switchIfEmpty(
                        itemRepository.findById(id)
                                .flatMap(item -> itemCacheService.cacheItem(id, item))
                )
                .flatMap(item -> {
                    if (userId == null) {
                        return Mono.just(itemMapper.toDto(item, 0));
                    }
                    return cartItemRepository.findByUserIdAndItemId(userId, item.getId())
                            .map(CartItem::getCount)
                            .defaultIfEmpty(0)
                            .map(count -> itemMapper.toDto(item, count));
                })
                .defaultIfEmpty(ItemDto.empty());
    }

    @Transactional
    public Mono<Void> updateItemCount(Long id, String action, Long userId) {
        if ("PLUS".equals(action)) {
            return cartItemRepository.findByUserIdAndItemId(userId, id)
                    .flatMap(cartItem -> {
                        cartItem.setCount(cartItem.getCount() + 1);
                        return cartItemRepository.save(cartItem);
                    })
                    .switchIfEmpty(Mono.defer(() ->
                            itemRepository.findById(id)
                                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Item " +
                                            "not found: " + id)))
                                    .flatMap(item -> {
                                        CartItem cartItem = new CartItem();
                                        cartItem.setItemId(item.getId());
                                        cartItem.setUserId(userId);
                                        cartItem.setCount(1);
                                        return cartItemRepository.save(cartItem);
                                    })
                    ))
                    .then();
        } else if ("MINUS".equals(action)) {
            return cartItemRepository.findByUserIdAndItemId(userId, id)
                    .flatMap(cartItem -> {
                        if (cartItem.getCount() > 1) {
                            cartItem.setCount(cartItem.getCount() - 1);
                            return cartItemRepository.save(cartItem).then();
                        } else {
                            return cartItemRepository.delete(cartItem);
                        }
                    })
                    .then();
        }
        return Mono.empty();
    }

    public Mono<PagingDto> getPaging(String search, int pageSize, int pageNumber) {
        return itemCacheService.getCachedCount(search)
                .switchIfEmpty(
                        itemRepository.countByTitleContainingIgnoreCase(search)
                                .flatMap(count -> itemCacheService.cacheCount(search, count))
                )
                .map(total -> {
                    int zeroBasedPage = pageNumber - 1;
                    boolean hasPrevious = zeroBasedPage > 0;
                    boolean hasNext = (long) (zeroBasedPage + 1) * pageSize < total;
                    return new PagingDto(pageNumber, pageSize, hasPrevious, hasNext);
                });
    }

    private Pageable buildPageable(int pageSize, int pageNumber, String sort) {
        int zeroBasedPage = pageNumber - 1;
        Sort sortOrder = switch (sort) {
            case "ALPHA" -> Sort.by("title");
            case "PRICE" -> Sort.by("price");
            default -> Sort.unsorted();
        };
        return PageRequest.of(zeroBasedPage, pageSize, sortOrder);
    }
}
