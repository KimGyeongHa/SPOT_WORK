package com.sukima.api.application.service;

import com.sukima.api.application.port.in.worker.RegisterWorkerUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.UserJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerService 단위 테스트")
class WorkerServiceTest {

    @InjectMocks
    private WorkerService workerService;

    @Mock
    private WorkerJpaRepository workerJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    private UserEntity workerUser;
    private UserEntity employerUser;

    @BeforeEach
    void setUp() {
        workerUser = UserEntity.builder()
                .id(1L)
                .email("worker@test.com")
                .password("encoded_password")
                .role(RoleType.WORKER)
                .build();

        employerUser = UserEntity.builder()
                .id(2L)
                .email("employer@test.com")
                .password("encoded_password")
                .role(RoleType.EMPLOYER)
                .build();
    }

    @Test
    @DisplayName("구직자 프로필 등록 성공")
    void register_success() {
        // given
        RegisterWorkerUseCase.Command command = new RegisterWorkerUseCase.Command(
                1L, "홍길동", "010-1234-5678"
        );

        given(workerJpaRepository.existsByUserId(command.userId())).willReturn(false);
        given(userJpaRepository.findById(command.userId())).willReturn(Optional.of(workerUser));
        given(workerJpaRepository.save(any(WorkerEntity.class))).willReturn(
                WorkerEntity.builder()
                        .id(1L)
                        .user(workerUser)
                        .name(command.name())
                        .phone(command.phone())
                        .build()
        );

        // when
        Long workerId = workerService.register(command);

        // then
        assertThat(workerId).isEqualTo(1L);
        verify(workerJpaRepository).save(any(WorkerEntity.class));
    }

    @Test
    @DisplayName("구직자 프로필 등록 실패 - 중복 프로필")
    void register_fail_duplicate_profile() {
        // given
        RegisterWorkerUseCase.Command command = new RegisterWorkerUseCase.Command(
                1L, "홍길동", "010-1234-5678"
        );

        given(workerJpaRepository.existsByUserId(command.userId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> workerService.register(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_WORKER_PROFILE);
                });

        verify(workerJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("구직자 프로필 등록 실패 - 존재하지 않는 유저")
    void register_fail_user_not_found() {
        // given
        RegisterWorkerUseCase.Command command = new RegisterWorkerUseCase.Command(
                999L, "홍길동", "010-1234-5678"
        );

        given(workerJpaRepository.existsByUserId(command.userId())).willReturn(false);
        given(userJpaRepository.findById(command.userId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workerService.register(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });

        verify(workerJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("구직자 프로필 등록 실패 - EMPLOYER 역할로 등록 시도")
    void register_fail_invalid_role() {
        // given
        RegisterWorkerUseCase.Command command = new RegisterWorkerUseCase.Command(
                2L, "홍길동", "010-1234-5678"
        );

        given(workerJpaRepository.existsByUserId(command.userId())).willReturn(false);
        given(userJpaRepository.findById(command.userId())).willReturn(Optional.of(employerUser));

        // when & then
        assertThatThrownBy(() -> workerService.register(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_ROLE);
                });

        verify(workerJpaRepository, never()).save(any());
    }
}
