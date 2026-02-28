package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.TestcontainersConfiguration;
import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.dto.PagingDto;
import com.my.project.mymarketapp.repository.CartItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ItemsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartItemRepository cartItemRepository;

    @AfterEach
    void cleanup() {
        cartItemRepository.deleteAll();
    }

    @Test
    void getItems_returnsItemsPage() throws Exception {
        MvcResult result = mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<List<ItemDto>> items =
                (List<List<ItemDto>>) result.getModelAndView().getModel().get("items");

        assertThat(items).isNotEmpty();
    }

    @Test
    void getItems_withSearch() throws Exception {
        MvcResult result = mockMvc.perform(get("/items").param("search", "Keyboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<List<ItemDto>> rows =
                (List<List<ItemDto>>) result.getModelAndView().getModel().get("items");

        // Flatten all non-empty placeholder items and verify every one contains "Keyboard"
        List<ItemDto> realItems = rows.stream()
                .flatMap(List::stream)
                .filter(dto -> dto.id() != -1L)
                .toList();

        assertThat(realItems).isNotEmpty();
        assertThat(realItems).allMatch(dto ->
                dto.title().toLowerCase().contains("keyboard"));
    }

    @Test
    void getItems_withPagination() throws Exception {
        MvcResult result = mockMvc.perform(get("/items")
                        .param("pageSize", "2")
                        .param("pageNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("paging"))
                .andReturn();

        PagingDto paging = (PagingDto) result.getModelAndView().getModel().get("paging");
        assertThat(paging.pageSize()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        List<List<ItemDto>> rows =
                (List<List<ItemDto>>) result.getModelAndView().getModel().get("items");

        long realItemCount = rows.stream()
                .flatMap(List::stream)
                .filter(dto -> dto.id() != -1L)
                .count();

        assertThat(realItemCount).isLessThanOrEqualTo(2);
    }

    @Test
    void updateItemCount_addsToCart() throws Exception {
        // POST /items with id=1 and action=PLUS should redirect back to /items
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());

        // Then GET /items/1 and verify the item has count=1 in the cart
        MvcResult result = mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andReturn();

        ItemDto item = (ItemDto) result.getModelAndView().getModel().get("item");
        assertThat(item.id()).isEqualTo(1L);
        assertThat(item.count()).isEqualTo(1);
    }

    @Test
    void getItem_returnsItemDetail() throws Exception {
        MvcResult result = mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andReturn();

        ItemDto item = (ItemDto) result.getModelAndView().getModel().get("item");
        assertThat(item.id()).isEqualTo(1L);
    }
}
