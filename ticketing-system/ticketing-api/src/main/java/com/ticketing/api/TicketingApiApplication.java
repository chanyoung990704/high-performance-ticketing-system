package com.ticketing.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.ticketing.domain")
@EntityScan(basePackages = "com.ticketing.domain")
@SpringBootApplication(scanBasePackages = "com.ticketing")
public class TicketingApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketingApiApplication.class, args);
    }
}
