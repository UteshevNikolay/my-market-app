package com.my.project.mymarketapp.mapper;

import com.my.project.mymarketapp.dto.OrderDto;
import com.my.project.mymarketapp.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderMapper {

    @Mapping(source = "items", target = "items", qualifiedByName = "orderItemToDto")
    @Mapping(target = "totalSum", expression = "java(order.getItems().stream().mapToInt(oi -> oi.getPrice() * oi.getCount()).sum())")
    OrderDto toDto(Order order);
}
