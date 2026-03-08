package com.my.project.paymentservice.controller;

import com.my.project.paymentservice.dto.PaymentRequest;
import com.my.project.paymentservice.dto.PaymentResponse;
import com.my.project.paymentservice.dto.BalanceResponse;
import com.my.project.paymentservice.generated.api.PaymentApi;
import com.my.project.paymentservice.service.PaymentService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Mono<BalanceResponse> getBalance(ServerWebExchange exchange) {
        return paymentService.getBalance();
    }

    @Override
    public Mono<PaymentResponse> processPayment(Mono<PaymentRequest> paymentRequest,
                                                 ServerWebExchange exchange) {
        return paymentRequest.flatMap(paymentService::processPayment);
    }
}
