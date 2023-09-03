package com.example.transactionaltestintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TransactionalTestIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionalTestIntegrationApplication.class, args);
    }

}
