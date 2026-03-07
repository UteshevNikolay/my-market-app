package com.my.project.mymarketapp.mapper;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.entity.Item;
import com.my.project.mymarketapp.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(source = "item.id", target = "id")
    @Mapping(source = "item.title", target = "title")
    @Mapping(source = "item.description", target = "description")
    @Mapping(source = "item.price", target = "price")
    @Mapping(source = "item.imgPath", target = "imgPath")
    @Mapping(source = "count", target = "count")
    ItemDto toDto(Item item, Integer count);

    @Mapping(source = "item.id", target = "id")
    @Mapping(source = "item.title", target = "title")
    @Mapping(source = "item.description", target = "description")
    @Mapping(source = "orderItem.price", target = "price")
    @Mapping(source = "item.imgPath", target = "imgPath")
    @Mapping(source = "orderItem.count", target = "count")
    ItemDto orderItemToDto(Item item, OrderItem orderItem);
}
