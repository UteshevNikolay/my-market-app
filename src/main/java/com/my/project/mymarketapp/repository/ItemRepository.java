package com.my.project.mymarketapp.repository;

import com.my.project.mymarketapp.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
