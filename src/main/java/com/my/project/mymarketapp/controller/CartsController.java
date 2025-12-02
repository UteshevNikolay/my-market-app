package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.service.CartsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CartsController {

    private final CartsService cartsService;

    public CartsController(CartsService cartsService) {
        this.cartsService = cartsService;
    }

    @GetMapping("/cart/items")
    public String getCartItems(Model model) {
        List<ItemDto> items = cartsService.getCartItems();
        int total = cartsService.getTotal();

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/cart/items")
    public String updateCartItem(
            @RequestParam Long id,
            @RequestParam String action
    ) {
        cartsService.updateCartItem(id, action);
        return "redirect:/cart/items";
    }

    @PostMapping("/buy")
    public String buy() {
        Long orderId = cartsService.createOrder();
        return "redirect:/orders/" + orderId + "?newOrder=true";
    }
}
