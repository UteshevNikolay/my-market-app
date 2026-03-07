package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.TestcontainersConfiguration;
import com.my.project.mymarketapp.dto.ItemDto;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.OrderItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CartsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanup() {
        // Order items must be deleted before orders due to FK constraint,
        // and cart items are independent.
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
    }

    @Test
    void getCartItems_emptyCart() throws Exception {
        MvcResult result = mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ItemDto> items = (List<ItemDto>) result.getModelAndView().getModel().get("items");
        Integer total = (Integer) result.getModelAndView().getModel().get("total");

        assertThat(items).isEmpty();
        assertThat(total).isEqualTo(0);
    }

    @Test
    void addAndViewCart() throws Exception {
        // Add item 1 to cart via POST /items
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());

        // GET /cart/items should show the item with count=1 and correct total
        MvcResult result = mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ItemDto> items = (List<ItemDto>) result.getModelAndView().getModel().get("items");
        Integer total = (Integer) result.getModelAndView().getModel().get("total");

        assertThat(items).hasSize(1);

        ItemDto cartItem = items.get(0);
        assertThat(cartItem.id()).isEqualTo(1L);
        assertThat(cartItem.count()).isEqualTo(1);

        // Total should equal item price * count (price * 1)
        assertThat(total).isEqualTo(cartItem.price() * cartItem.count());
    }

    @Test
    void updateCartItem_delete() throws Exception {
        // First add item 1 to cart
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());

        // Then delete it via POST /cart/items with action=DELETE
        mockMvc.perform(post("/cart/items")
                        .param("id", "1")
                        .param("action", "DELETE"))
                .andExpect(status().is3xxRedirection());

        // GET /cart/items should now show empty cart
        MvcResult result = mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ItemDto> items = (List<ItemDto>) result.getModelAndView().getModel().get("items");
        assertThat(items).isEmpty();
    }

    @Test
    void buy_createsOrder() throws Exception {
        // Add item 1 to cart
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());

        // POST /buy should redirect to /orders/{id}?newOrder=true
        MvcResult buyResult = mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        ".*/orders/\\d+\\?newOrder=true")))
                .andReturn();

        // Cart should now be empty after purchase
        MvcResult cartResult = mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ItemDto> cartItems = (List<ItemDto>) cartResult.getModelAndView().getModel().get(
                "items");
        assertThat(cartItems).isEmpty();
    }
}
