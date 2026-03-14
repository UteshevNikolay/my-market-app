package com.my.project.mymarketapp.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    private Long id;

    private String username;

    private String password;
}
