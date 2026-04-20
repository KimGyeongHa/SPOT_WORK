package com.sukima.api.domain.payment;

import com.sukima.api.domain.payment.type.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Payment {

    private final Long id;
    private final Long matchId;
    private final Long workerId;
    private final int amount;
    private final PaymentStatus status;
    private final LocalDateTime paidAt;

    @Builder
    public Payment(Long id, Long matchId, Long workerId,
                   int amount, PaymentStatus status, LocalDateTime paidAt) {
        this.id = id;
        this.matchId = matchId;
        this.workerId = workerId;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
    }
}
