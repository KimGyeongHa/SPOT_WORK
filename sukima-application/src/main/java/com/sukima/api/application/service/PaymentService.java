package com.sukima.api.application.service;

import com.sukima.api.application.port.in.payment.CompletePaymentUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.infrastructure.persistence.entity.payment.PaymentEntity;
import com.sukima.api.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService implements CompletePaymentUseCase {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    @Transactional
    public void complete(Long paymentId) {
        PaymentEntity payment = findPayment(paymentId);
        validatePending(payment);
        payment.complete();
        log.info("정산 완료: paymentId={}, amount={}", paymentId, payment.getAmount());
    }

    @Override
    @Transactional
    public void fail(Long paymentId) {
        PaymentEntity payment = findPayment(paymentId);
        validatePending(payment);
        payment.fail();
        log.warn("정산 실패: paymentId={}, amount={}", paymentId, payment.getAmount());
    }

    private PaymentEntity findPayment(Long paymentId) {
        return paymentJpaRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validatePending(PaymentEntity payment) {
        if (!payment.isPending()) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }
}
