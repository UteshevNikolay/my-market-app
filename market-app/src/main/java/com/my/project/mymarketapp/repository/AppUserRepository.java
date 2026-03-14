package com.my.project.mymarketapp.repository;

import com.my.project.mymarketapp.entity.AppUser;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AppUserRepository extends ReactiveCrudRepository<AppUser, Long> {
    Mono<AppUser> findByUsername(String username);
}
