package com.sukima.api.application.service;

import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.payment.type.PaymentStatus;
import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import com.sukima.api.infrastructure.persistence.entity.payment.PaymentEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.PaymentJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentJpaRepository paymentJpaRepository;

    private PaymentEntity pendingPayment;
    private PaymentEntity completedPayment;

    @BeforeEach
    void setUp() {
        pendingPayment = PaymentEntity.builder()
                .id(1L)
                .match(mock(MatchEntity.class))
                .worker(mock(WorkerEntity.class))
                .amount(40000)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        completedPayment = PaymentEntity.builder()
                .id(2L)
                .match(mock(MatchEntity.class))
                .worker(mock(WorkerEntity.class))
                .amount(40000)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("정산 완료 처리 성공")
    void complete_success() {
        given(paymentJpaRepository.findById(1L)).willReturn(Optional.of(pendingPayment));

        paymentService.complete(1L);

        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(pendingPayment.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("정산 완료 실패 - 존재하지 않는 정산")
    void complete_fail_not_found() {
        given(paymentJpaRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.complete(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("정산 완료 실패 - 이미 완료된 정산")
    void complete_fail_already_completed() {
        given(paymentJpaRepository.findById(2L)).willReturn(Optional.of(completedPayment));

        assertThatThrownBy(() -> paymentService.complete(2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS));
    }

    @Test
    @DisplayName("정산 실패 처리 성공")
    void fail_success() {
        given(paymentJpaRepository.findById(1L)).willReturn(Optional.of(pendingPayment));

        paymentService.fail(1L);

        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("정산 실패 처리 실패 - 이미 완료된 정산")
    void fail_fail_already_completed() {
        given(paymentJpaRepository.findById(2L)).willReturn(Optional.of(completedPayment));

        assertThatThrownBy(() -> paymentService.fail(2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS));
    }
}
