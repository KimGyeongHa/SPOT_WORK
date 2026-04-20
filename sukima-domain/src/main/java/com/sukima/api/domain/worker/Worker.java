package com.sukima.api.domain.worker;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class Worker {

    private final Long id;
    private final Long userId;
    private final String name;
    private final String phone;
    private final BigDecimal rating;

    @Builder
    public Worker(Long id, Long userId, String name, String phone, BigDecimal rating) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.rating = rating;
    }
}
