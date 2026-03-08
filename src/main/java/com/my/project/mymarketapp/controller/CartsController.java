package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.dto.ActionForm;
import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.service.CartsService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

@Controller
public class CartsController {

    private final CartsService cartsService;

    public CartsController(CartsService cartsService) {
        this.cartsService = cartsService;
    }

    @GetMapping("/cart/items")
    public Mono<String> getCartItems(Model model) {
        Mono<List<ItemDto>> itemsMono = cartsService.getCartItems().collectList();
        Mono<Integer> totalMono = cartsService.getTotal();

        return Mono.zip(itemsMono, totalMono)
                .doOnNext(tuple -> {
                    model.addAttribute("items", tuple.getT1());
                    model.addAttribute("total", tuple.getT2());
                })
                .thenReturn("cart");
    }

    @PostMapping("/cart/items")
    public Mono<String> updateCartItem(@ModelAttribute ActionForm form) {
        return cartsService.updateCartItem(form.getId(), form.getAction())
                .thenReturn("redirect:/cart/items");
    }

    @PostMapping("/buy")
    public Mono<String> buy() {
        return cartsService.createOrder()
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }
}
