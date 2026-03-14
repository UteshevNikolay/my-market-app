package com.my.project.mymarketapp.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("cart_item")
@Getter
@Setter
@NoArgsConstructor
public class CartItem {

    @Id
    private Long id;

    @Column("item_id")
    private Long itemId;

    @Column("user_id")
    private Long userId;

    private Integer count;
}
