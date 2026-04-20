package com.sukima.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.sukima.api.infrastructure.persistence.entity"})
@EnableJpaRepositories(basePackages = {"com.sukima.api.infrastructure.persistence.repository"})
public class ApiSukimaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiSukimaApplication.class, args);
    }
}
