package com.sukima.api.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.sukima.api.batch",
        "com.sukima.api.infrastructure"
})
@EnableScheduling
@EntityScan(basePackages = "com.sukima.api.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.sukima.api.infrastructure.persistence.repository")
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
