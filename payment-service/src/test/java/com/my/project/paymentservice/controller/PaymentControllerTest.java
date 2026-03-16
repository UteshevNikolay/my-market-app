package com.my.project.paymentservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest
class PaymentControllerTest {

    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void getBalance_returns200WithBalance() {
        webTestClient.mutateWith(mockJwt()).get().uri("/api/payment/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isNumber();
    }

    @Test
    void processPayment_success_returns200WithDeductedBalance() {
        // First get the current balance
        Integer initialBalance = webTestClient.mutateWith(mockJwt()).get().uri("/api/payment" +
                        "/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(java.util.Map.class)
                .returnResult()
                .getResponseBody()
                .get("balance") instanceof Number n ? n.intValue() : 0;

        // Make a payment
        webTestClient.mutateWith(mockJwt()).post().uri("/api/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\": 1000}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.balance").isEqualTo(initialBalance - 1000);
    }

    @Test
    void processPayment_insufficientFunds_returns200WithFailure() {
        webTestClient.mutateWith(mockJwt()).post().uri("/api/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\": 999999999}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isNotEmpty();
    }

    @Test
    void processPayment_negativeAmount_returnsFalse() {
        webTestClient.mutateWith(mockJwt()).post().uri("/api/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\": -100}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }
}
