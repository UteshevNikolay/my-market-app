package com.my.project.mymarketapp.service;

import com.my.project.mymarketapp.dto.ItemDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartsService {

    public List<ItemDto> getCartItems() {
        // TODO: Implement actual logic
        return List.of();
    }

    public void updateCartItem(Long id, String action) {
        // TODO: Implement actual logic
    }

    public int getTotal() {
        // TODO: Implement actual logic
        return 0;
    }

    public Long createOrder() {
        // TODO: Implement actual logic
        return null;
    }
}

