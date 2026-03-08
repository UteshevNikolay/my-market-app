package com.my.project.mymarketapp.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("customer_order")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    private Long id;
}
