package com.my.project.mymarketapp.service;

import com.my.project.payment.client.api.PaymentApi;
import com.my.project.payment.client.model.PaymentRequest;
import com.my.project.payment.client.model.PaymentResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaymentClientService {

    private final PaymentApi paymentApi;

    public PaymentClientService(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    public Mono<Integer> getBalance() {
        return paymentApi.getBalance()
                .map(response -> response.getBalance())
                .onErrorReturn(-1);
    }

    public Mono<PaymentResponse> processPayment(int amount) {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(amount);
        return paymentApi.processPayment(request)
                .onErrorResume(e -> {
                    PaymentResponse errorResponse = new PaymentResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setMessage("Сервис платежей недоступен");
                    return Mono.just(errorResponse);
                });
    }
}
