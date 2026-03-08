package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.dto.ActionForm;
import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.service.CartsService;
import com.my.project.mymarketapp.service.PaymentClientService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

@Controller
public class CartsController {

    private final CartsService cartsService;
    private final PaymentClientService paymentClientService;

    public CartsController(CartsService cartsService, PaymentClientService paymentClientService) {
        this.cartsService = cartsService;
        this.paymentClientService = paymentClientService;
    }

    @GetMapping("/cart/items")
    public Mono<String> getCartItems(Model model,
            @RequestParam(required = false) String error) {
        Mono<List<ItemDto>> itemsMono = cartsService.getCartItems().collectList();
        Mono<Integer> totalMono = cartsService.getTotal();
        Mono<Integer> balanceMono = paymentClientService.getBalance();

        return Mono.zip(itemsMono, totalMono, balanceMono)
                .doOnNext(tuple -> {
                    List<ItemDto> items = tuple.getT1();
                    int total = tuple.getT2();
                    int balance = tuple.getT3();

                    model.addAttribute("items", items);
                    model.addAttribute("total", total);
                    model.addAttribute("balance", balance);
                    model.addAttribute("paymentAvailable", balance >= 0);
                    model.addAttribute("canBuy", balance >= 0 && balance >= total);
                    if (error != null) {
                        model.addAttribute("error", error);
                    }
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
        return cartsService.getTotal()
                .flatMap(total -> paymentClientService.processPayment(total))
                .flatMap(response -> {
                    if (Boolean.TRUE.equals(response.getSuccess())) {
                        return cartsService.createOrder()
                                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
                    } else {
                        String msg = URLEncoder.encode(
                                response.getMessage() != null ? response.getMessage() : "Оплата не прошла",
                                StandardCharsets.UTF_8);
                        return Mono.just("redirect:/cart/items?error=" + msg);
                    }
                });
    }
}
