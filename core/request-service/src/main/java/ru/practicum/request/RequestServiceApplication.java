package ru.practicum.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {
                "ru.practicum.request",
                "ru.practicum.main"
        }
)
@EntityScan(basePackages = "ru.practicum.main.model")
@EnableJpaRepositories(basePackages = "ru.practicum.main.repository")
@EnableFeignClients(basePackages = "ru.practicum.interaction.client")
public class RequestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                RequestServiceApplication.class,
                args
        );
    }
}