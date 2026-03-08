package com.my.project.paymentservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("payment")
public class PaymentProperties {
    private int initialBalance = 100000;
}
