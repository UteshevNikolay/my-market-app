package com.my.project.mymarketapp.config;

import com.my.project.payment.client.ApiClient;
import com.my.project.payment.client.api.PaymentApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentClientConfig {

    @Bean
    public PaymentApi paymentApi(@Value("${payment.service.url}") String baseUrl) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);
        return new PaymentApi(apiClient);
    }
}
