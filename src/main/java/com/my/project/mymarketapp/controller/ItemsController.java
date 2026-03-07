package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.dto.ActionForm;
import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.dto.PagingDto;
import com.my.project.mymarketapp.service.ItemsService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

@Controller
public class ItemsController {

    private final ItemsService itemsService;

    public ItemsController(ItemsService itemsService) {
        this.itemsService = itemsService;
    }

    @GetMapping("/items")
    public Mono<String> getItems(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            Model model
    ) {
        Mono<List<ItemDto>> itemsMono = itemsService.getItems(search, sort, pageSize, pageNumber).collectList();
        Mono<PagingDto> pagingMono = itemsService.getPaging(search, pageSize, pageNumber);

        return Mono.zip(itemsMono, pagingMono)
                .doOnNext(tuple -> {
                    List<ItemDto> flatList = tuple.getT1();
                    List<List<ItemDto>> rows = new ArrayList<>();
                    int rowSize = 4;
                    for (int i = 0; i < flatList.size(); i += rowSize) {
                        List<ItemDto> row = new ArrayList<>(flatList.subList(i, Math.min(i + rowSize, flatList.size())));
                        while (row.size() < rowSize) {
                            row.add(ItemDto.empty());
                        }
                        rows.add(row);
                    }
                    model.addAttribute("items", rows);
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort);
                    model.addAttribute("paging", tuple.getT2());
                })
                .thenReturn("items");
    }

    @PostMapping("/items")
    public Mono<String> updateItemCount(@ModelAttribute ActionForm form) {
        return itemsService.updateItemCount(form.getId(), form.getAction())
                .thenReturn("redirect:/items?search=" + form.getSearch() + "&sort=" + form.getSort() + "&pageSize=" + form.getPageSize() + "&pageNumber=" + form.getPageNumber());
    }

    @GetMapping("/items/{id}")
    public Mono<String> getItem(@PathVariable Long id, Model model) {
        return itemsService.getItemById(id)
                .doOnNext(item -> model.addAttribute("item", item))
                .thenReturn("item");
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateItemCountById(@PathVariable Long id, @ModelAttribute ActionForm form) {
        return itemsService.updateItemCount(id, form.getAction())
                .thenReturn("redirect:/items/" + id);
    }
}
