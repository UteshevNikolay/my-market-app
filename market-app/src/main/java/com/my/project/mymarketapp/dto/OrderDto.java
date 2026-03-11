package com.my.project.mymarketapp.dto;

import java.util.List;

public record OrderDto(
        Long id,
        List<ItemDto> items,
        Integer totalSum
) {
}

