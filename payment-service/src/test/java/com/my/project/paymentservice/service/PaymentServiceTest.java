package com.my.project.paymentservice.service;

import com.my.project.paymentservice.config.PaymentProperties;
import com.my.project.paymentservice.dto.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        properties.setInitialBalance(10000);
        paymentService = new PaymentService(properties);
    }

    @Test
    void getBalance_returnsInitialBalance() {
        StepVerifier.create(paymentService.getBalance())
                .assertNext(response -> assertThat(response.getBalance()).isEqualTo(10000))
                .verifyComplete();
    }

    @Test
    void processPayment_sufficientFunds_deductsAndReturnsSuccess() {
        StepVerifier.create(paymentService.processPayment(new PaymentRequest().amount(3000)))
                .assertNext(response -> {
                    assertThat(response.getSuccess()).isTrue();
                    assertThat(response.getBalance()).isEqualTo(7000);
                    assertThat(response.getMessage()).isNull();
                })
                .verifyComplete();

        // Verify balance was actually deducted
        StepVerifier.create(paymentService.getBalance())
                .assertNext(response -> assertThat(response.getBalance()).isEqualTo(7000))
                .verifyComplete();
    }

    @Test
    void processPayment_insufficientFunds_returnsFalse() {
        StepVerifier.create(paymentService.processPayment(new PaymentRequest().amount(20000)))
                .assertNext(response -> {
                    assertThat(response.getSuccess()).isFalse();
                    assertThat(response.getBalance()).isEqualTo(10000);
                    assertThat(response.getMessage()).isNotNull();
                })
                .verifyComplete();

        // Verify balance unchanged
        StepVerifier.create(paymentService.getBalance())
                .assertNext(response -> assertThat(response.getBalance()).isEqualTo(10000))
                .verifyComplete();
    }

    @Test
    void processPayment_exactBalance_deductsToZero() {
        StepVerifier.create(paymentService.processPayment(new PaymentRequest().amount(10000)))
                .assertNext(response -> {
                    assertThat(response.getSuccess()).isTrue();
                    assertThat(response.getBalance()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    void processPayment_zeroOrNegativeAmount_returnsFalse() {
        StepVerifier.create(paymentService.processPayment(new PaymentRequest().amount(0)))
                .assertNext(response -> assertThat(response.getSuccess()).isFalse())
                .verifyComplete();

        StepVerifier.create(paymentService.processPayment(new PaymentRequest().amount(-100)))
                .assertNext(response -> assertThat(response.getSuccess()).isFalse())
                .verifyComplete();
    }
}
