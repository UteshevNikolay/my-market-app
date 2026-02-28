package com.my.project.mymarketapp.controller;

import com.my.project.mymarketapp.TestcontainersConfiguration;
import com.my.project.mymarketapp.dto.OrderDto;
import com.my.project.mymarketapp.repository.CartItemRepository;
import com.my.project.mymarketapp.repository.OrderItemRepository;
import com.my.project.mymarketapp.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
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
class OrdersControllerIntegrationTest {

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
        // Order items must be deleted before orders due to FK constraint.
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
    }

    /**
     * Helper: adds item with the given id to the cart, then calls POST /buy and returns the order id
     * extracted from the redirect Location header.
     */
    private Long addItemAndBuy(long itemId) throws Exception {
        mockMvc.perform(post("/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection());

        MvcResult buyResult = mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Location header is like: /orders/42?newOrder=true
        String location = buyResult.getResponse().getHeader("Location");
        assertThat(location).isNotNull();

        // Extract the numeric order id from the redirect URL
        String path = location.contains("?") ? location.substring(0, location.indexOf('?')) : location;
        String idStr = path.substring(path.lastIndexOf('/') + 1);
        return Long.parseLong(idStr);
    }

    @Test
    void getOrders_empty() throws Exception {
        MvcResult result = mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<OrderDto> orders = (List<OrderDto>) result.getModelAndView().getModel().get("orders");
        assertThat(orders).isEmpty();
    }

    @Test
    void getOrders_afterPurchase() throws Exception {
        addItemAndBuy(1L);

        MvcResult result = mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<OrderDto> orders = (List<OrderDto>) result.getModelAndView().getModel().get("orders");
        assertThat(orders).hasSize(1);
    }

    @Test
    void getOrder_returnsOrderDetail() throws Exception {
        Long orderId = addItemAndBuy(1L);

        // GET /orders/{id} without ?newOrder param (defaults to false)
        MvcResult result = mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attributeExists("newOrder"))
                .andReturn();

        OrderDto order = (OrderDto) result.getModelAndView().getModel().get("order");
        Boolean newOrder = (Boolean) result.getModelAndView().getModel().get("newOrder");

        assertThat(order.id()).isEqualTo(orderId);
        assertThat(order.items()).isNotEmpty();
        assertThat(order.totalSum()).isGreaterThan(0);
        assertThat(newOrder).isFalse();
    }
}
