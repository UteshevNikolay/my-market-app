package com.my.project.mymarketapp.dto;

public record PagingDto(
        int pageNumber,
        int pageSize,
        boolean hasPrevious,
        boolean hasNext
) {
}

