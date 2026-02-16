package com.strataguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.strataguard")
public class StrataguardApplication {

    public static void main(String[] args) {
        SpringApplication.run(StrataguardApplication.class, args);
    }
}
