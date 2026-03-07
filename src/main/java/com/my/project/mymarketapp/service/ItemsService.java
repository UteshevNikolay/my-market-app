package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.dto.PagingDto;
import com.my.project.mymarketapp.entity.CartItem;
import com.my.project.mymarketapp.entity.Item;
import com.my.project.mymarketapp.mapper.ItemMapper;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ItemsService {

    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemMapper itemMapper;

    public ItemsService(ItemRepository itemRepository,
                        CartItemRepository cartItemRepository,
                        ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.cartItemRepository = cartItemRepository;
        this.itemMapper = itemMapper;
    }

    public List<ItemDto> getItems(String search, String sort, int pageSize, int pageNumber) {
        Pageable pageable = buildPageable(pageSize, pageNumber, sort);
        Page<Item> page = itemRepository.findByTitleContainingIgnoreCase(search, pageable);

        List<ItemDto> flatList = new ArrayList<>();
        for (Item item : page.getContent()) {
            int count = cartItemRepository.findByItemId(item.getId())
                    .map(CartItem::getCount)
                    .orElse(0);
            flatList.add(itemMapper.toDto(item, count));
        }

        return flatList;
    }

    public ItemDto getItemById(Long id) {
        Item item = itemRepository.findById(id).orElse(null);
        if (item == null) {
            return ItemDto.empty();
        }
        int count = cartItemRepository.findByItemId(item.getId())
                .map(CartItem::getCount)
                .orElse(0);
        return itemMapper.toDto(item, count);
    }

    @Transactional
    public void updateItemCount(Long id, String action) {
        Optional<CartItem> existing = cartItemRepository.findByItemId(id);
        if ("PLUS".equals(action)) {
            if (existing.isPresent()) {
                CartItem cartItem = existing.get();
                cartItem.setCount(cartItem.getCount() + 1);
                cartItemRepository.save(cartItem);
            } else {
                Item item = itemRepository.findById(id).orElseThrow(
                        () -> new IllegalArgumentException("Item not found: " + id));
                CartItem cartItem = new CartItem();
                cartItem.setItem(item);
                cartItem.setCount(1);
                cartItemRepository.save(cartItem);
            }
        } else if ("MINUS".equals(action)) {
            if (existing.isPresent()) {
                CartItem cartItem = existing.get();
                if (cartItem.getCount() > 1) {
                    cartItem.setCount(cartItem.getCount() - 1);
                    cartItemRepository.save(cartItem);
                } else {
                    cartItemRepository.delete(cartItem);
                }
            }
        }
    }

    public PagingDto getPaging(String search, int pageSize, int pageNumber) {
        Pageable pageable = buildPageable(pageSize, pageNumber, "NO");
        Page<Item> page = itemRepository.findByTitleContainingIgnoreCase(search, pageable);
        return new PagingDto(pageNumber, pageSize, page.hasPrevious(), page.hasNext());
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
