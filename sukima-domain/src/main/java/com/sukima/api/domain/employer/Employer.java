package com.sukima.api.domain.employer;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class Employer {

    private final Long id;
    private final Long userId;
    private final String name;
    private final String phone;
    private final String companyName;
    private final BigDecimal rating;

    @Builder
    public Employer(Long id, Long userId, String name, String phone, String companyName, BigDecimal rating) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.companyName = companyName;
        this.rating = rating;
    }
}
