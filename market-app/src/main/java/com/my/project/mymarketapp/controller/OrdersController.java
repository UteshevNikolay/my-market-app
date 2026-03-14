package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.security.AppUserDetails;
import com.my.project.mymarketapp.service.OrdersService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

@Controller
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @GetMapping("/orders")
    public Mono<String> getOrders(@AuthenticationPrincipal AppUserDetails user, Model model) {
        return ordersService.getAllOrders(user.getUserId()).collectList()
                .doOnNext(orders -> model.addAttribute("orders", orders))
                .thenReturn("orders");
    }

    @GetMapping("/orders/{id}")
    public Mono<String> getOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails user,
            @RequestParam(required = false, defaultValue = "false") boolean newOrder,
            Model model
    ) {
        return ordersService.getOrderById(id, user.getUserId())
                .doOnNext(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                })
                .thenReturn("order");
    }
}
