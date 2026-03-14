package com.my.project.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class PaymentServiceApplicationTest {

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void contextLoads() {
    }
}
