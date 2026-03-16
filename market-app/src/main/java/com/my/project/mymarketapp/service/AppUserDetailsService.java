package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.repository.AppUserRepository;
import com.my.project.mymarketapp.security.AppUserDetails;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsPasswordService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AppUserDetailsService implements ReactiveUserDetailsService,
        ReactiveUserDetailsPasswordService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .map(user -> new AppUserDetails(
                        user.getId(),
                        user.getUsername(),
                        user.getPassword(),
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                ))
                .cast(UserDetails.class)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(
                        "User not found: " + username)));
    }

    @Override
    public Mono<UserDetails> updatePassword(UserDetails user, String newPassword) {
        return Mono.just(user);
    }
}
