package com.ticketing.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = "com.ticketing")
public class TicketingApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketingApiApplication.class, args);
    }
}
