package com.my.project.mymarketapp;

import com.my.project.mymarketapp.config.RedisTestcontainersConfiguration;
import com.my.project.mymarketapp.config.TestcontainersConfiguration;
import com.my.project.mymarketapp.service.PaymentClientService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RedisTestcontainersConfiguration.class})
class MyMarketAppApplicationTest {

    @MockitoBean
    private PaymentClientService paymentClientService;

    @Test
    void contextLoads() {
    }
}
