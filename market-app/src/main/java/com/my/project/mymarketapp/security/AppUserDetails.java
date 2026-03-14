package com.my.project.mymarketapp.security;

import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class AppUserDetails extends User {

    private final Long userId;

    public AppUserDetails(Long userId, String username, String password,
                          Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
    }
}
