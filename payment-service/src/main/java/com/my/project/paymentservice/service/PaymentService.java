package com.my.project.paymentservice.service;

import com.my.project.paymentservice.config.PaymentProperties;
import com.my.project.paymentservice.dto.BalanceResponse;
import com.my.project.paymentservice.dto.PaymentRequest;
import com.my.project.paymentservice.dto.PaymentResponse;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaymentService {

    private final AtomicInteger balance;

    public PaymentService(PaymentProperties properties) {
        this.balance = new AtomicInteger(properties.getInitialBalance());
    }

    public Mono<BalanceResponse> getBalance() {
        return Mono.just(new BalanceResponse().balance(balance.get()));
    }

    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        return Mono.fromCallable(() -> {
            int amount = request.getAmount();
            if (amount <= 0) {
                return new PaymentResponse().success(false).balance(balance.get())
                        .message("Сумма платежа должна быть положительной");
            }
            while (true) {
                int current = balance.get();
                if (current < amount) {
                    return new PaymentResponse().success(false).balance(current)
                            .message("Недостаточно средств на счёте");
                }
                if (balance.compareAndSet(current, current - amount)) {
                    return new PaymentResponse().success(true).balance(current - amount);
                }
            }
        });
    }
}
