package com.my.project.mymarketapp.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("item")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    @Id
    private Long id;

    private String title;

    private String description;

    private Integer price;

    @Column("img_path")
    private String imgPath;
}
