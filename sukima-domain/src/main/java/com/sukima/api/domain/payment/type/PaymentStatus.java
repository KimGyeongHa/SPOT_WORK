package com.sukima.api.domain.payment.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("정산대기"),
    COMPLETED("정산완료"),
    FAILED("정산실패");

    private final String description;
}
