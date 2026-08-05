package ru.practicum.additional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "ru.practicum.additional",
        "ru.practicum.main"
})
@EntityScan(basePackages = "ru.practicum.main.model")
@EnableJpaRepositories(basePackages = "ru.practicum.main.repository")
public class AdditionalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                AdditionalServiceApplication.class,
                args
        );
    }
}