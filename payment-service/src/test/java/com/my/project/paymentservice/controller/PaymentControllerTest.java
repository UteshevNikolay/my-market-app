package com.my.project.paymentservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void getBalance_returns200WithBalance() {
        webTestClient.get().uri("/api/payment/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isNumber();
    }

    @Test
    void processPayment_success_returns200WithDeductedBalance() {
        // First get the current balance
        Integer initialBalance = webTestClient.get().uri("/api/payment/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(java.util.Map.class)
                .returnResult()
                .getResponseBody()
                .get("balance") instanceof Number n ? n.intValue() : 0;

        // Make a payment
        webTestClient.post().uri("/api/payment/pay")
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
        webTestClient.post().uri("/api/payment/pay")
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
        webTestClient.post().uri("/api/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\": -100}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }
}
