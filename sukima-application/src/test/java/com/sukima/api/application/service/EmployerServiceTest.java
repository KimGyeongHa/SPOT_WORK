package com.sukima.api.application.service;

import com.sukima.api.application.port.in.employer.RegisterEmployerUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.repository.EmployerJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.UserJpaRepository;
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
@DisplayName("EmployerService 단위 테스트")
class EmployerServiceTest {

    @InjectMocks
    private EmployerService employerService;

    @Mock
    private EmployerJpaRepository employerJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    private UserEntity employerUser;
    private UserEntity workerUser;

    @BeforeEach
    void setUp() {
        employerUser = UserEntity.builder()
                .id(1L)
                .email("employer@test.com")
                .password("encoded_password")
                .role(RoleType.EMPLOYER)
                .build();

        workerUser = UserEntity.builder()
                .id(2L)
                .email("worker@test.com")
                .password("encoded_password")
                .role(RoleType.WORKER)
                .build();
    }

    @Test
    @DisplayName("구인자 프로필 등록 성공")
    void register_success() {
        // given
        RegisterEmployerUseCase.Command command = new RegisterEmployerUseCase.Command(
                1L, "홍길동", "010-1234-5678", "스키마카페"
        );

        given(employerJpaRepository.existsByUserId(command.userId())).willReturn(false);
        given(userJpaRepository.findById(command.userId())).willReturn(Optional.of(employerUser));
        given(employerJpaRepository.save(any(EmployerEntity.class))).willReturn(
                EmployerEntity.builder()
                        .id(1L)
                        .user(employerUser)
                        .name(command.name())
                        .phone(command.phone())
                        .companyName(command.companyName())
                        .rating(BigDecimal.ZERO)
                        .build()
        );

        // when
        Long employerId = employerService.register(command);

        // then
        assertThat(employerId).isEqualTo(1L);
        verify(employerJpaRepository).save(any(EmployerEntity.class));
    }

    @Test
    @DisplayName("구인자 프로필 등록 실패 - 중복 프로필")
    void register_fail_duplicate_profile() {
        // given
        RegisterEmployerUseCase.Command command = new RegisterEmployerUseCase.Command(
                1L, "홍길동", "010-1234-5678", "스키마카페"
        );

        given(employerJpaRepository.existsByUserId(command.userId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> employerService.register(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMPLOYER_PROFILE);
                });

        verify(employerJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("구인자 프로필 등록 실패 - 존재하지 않는 유저")
    void register_fail_user_not_found() {
        // given
        RegisterEmployerUseCase.Command command = new RegisterEmployerUseCase.Command(
                999L, "홍길동", "010-1234-5678", "스키마카페"
        );

        given(employerJpaRepository.existsByUserId(command.userId())).willReturn(false);
        given(userJpaRepository.findById(command.userId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> employerService.register(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });

        verify(employerJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("구인자 프로필 등록 실패 - WORKER 역할로 등록 시도")
    void register_fail_invalid_role() {
        // given
        RegisterEmployerUseCase.Command command = new RegisterEmployerUseCase.Command(
                2L, "홍길동", "010-1234-5678", "스키마카페"
        );

        given(employerJpaRepository.existsByUserId(command.userId())).willReturn(false);
        given(userJpaRepository.findById(command.userId())).willReturn(Optional.of(workerUser));

        // when & then
        assertThatThrownBy(() -> employerService.register(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_ROLE);
                });

        verify(employerJpaRepository, never()).save(any());
    }
}
