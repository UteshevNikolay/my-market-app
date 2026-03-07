package com.my.project.mymarketapp.dto;

public record ItemDto(
        Long id,
        String title,
        String description,
        Integer price,
        String imgPath,
        Integer count
) {
    public static ItemDto empty() {
        return new ItemDto(-1L, "", "", 0, "", 0);
    }
}

