package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.dto.PagingDto;
import com.my.project.mymarketapp.service.ItemsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ItemsController {

    private final ItemsService itemsService;

    public ItemsController(ItemsService itemsService) {
        this.itemsService = itemsService;
    }

    @GetMapping("/items")
    public String getItems(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            Model model
    ) {
        List<List<ItemDto>> items = itemsService.getItems(search, sort, pageSize, pageNumber);
        PagingDto paging = itemsService.getPaging(search, pageSize, pageNumber);

        model.addAttribute("items", items);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", paging);

        return "items";
    }

    @PostMapping("/items")
    public String updateItemCount(
            @RequestParam Long id,
            @RequestParam String action,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "1") int pageNumber
    ) {
        itemsService.updateItemCount(id, action);
        return "redirect:/items?search=" + search + "&sort=" + sort + "&pageSize=" + pageSize + "&pageNumber=" + pageNumber;
    }

    @GetMapping("/items/{id}")
    public String getItem(@PathVariable Long id, Model model) {
        ItemDto item = itemsService.getItemById(id);
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/items/{id}")
    public String updateItemCountById(@PathVariable Long id, @RequestParam String action) {
        itemsService.updateItemCount(id, action);
        return "redirect:/items/" + id;
    }
}
