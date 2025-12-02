package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.dto.PagingDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemsService {

    public List<List<ItemDto>> getItems(String search, String sort, int pageSize, int pageNumber) {
        // TODO: Implement actual logic
        return List.of();
    }

    public ItemDto getItemById(Long id) {
        // TODO: Implement actual logic
        return ItemDto.empty();
    }

    public void updateItemCount(Long id, String action) {
        // TODO: Implement actual logic
    }

    public PagingDto getPaging(String search, int pageSize, int pageNumber) {
        // TODO: Implement actual logic
        return new PagingDto(pageNumber, pageSize, false, false);
    }
}

