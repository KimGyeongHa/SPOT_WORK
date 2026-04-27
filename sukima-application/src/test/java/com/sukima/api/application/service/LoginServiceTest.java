package com.sukima.api.application.service;

import com.sukima.api.application.port.in.user.LoginUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginService 단위 테스트")
class LoginServiceTest {

    @InjectMocks
    private LoginService loginService;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
    @DisplayName("로그인 성공 - WORKER")
    void login_success_worker() {
        // given
        LoginUseCase.Command command = new LoginUseCase.Command("worker@test.com", "password123");

        given(userJpaRepository.findByEmail(command.email())).willReturn(Optional.of(workerUser));
        given(passwordEncoder.matches(command.password(), workerUser.getPassword())).willReturn(true);

        // when
        LoginUseCase.Result result = loginService.login(command);

        // then
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("WORKER");
        assertThat(result.email()).isEqualTo("worker@test.com");
    }

    @Test
    @DisplayName("로그인 성공 - EMPLOYER")
    void login_success_employer() {
        // given
        LoginUseCase.Command command = new LoginUseCase.Command("employer@test.com", "password123");

        given(userJpaRepository.findByEmail(command.email())).willReturn(Optional.of(employerUser));
        given(passwordEncoder.matches(command.password(), employerUser.getPassword())).willReturn(true);

        // when
        LoginUseCase.Result result = loginService.login(command);

        // then
        assertThat(result.userId()).isEqualTo(2L);
        assertThat(result.role()).isEqualTo("EMPLOYER");
        assertThat(result.email()).isEqualTo("employer@test.com");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_fail_email_not_found() {
        // given
        LoginUseCase.Command command = new LoginUseCase.Command("notexist@test.com", "password123");

        given(userJpaRepository.findByEmail(command.email())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loginService.login(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_LOGIN);
                });
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrong_password() {
        // given
        LoginUseCase.Command command = new LoginUseCase.Command("worker@test.com", "wrongPassword");

        given(userJpaRepository.findByEmail(command.email())).willReturn(Optional.of(workerUser));
        given(passwordEncoder.matches(command.password(), workerUser.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> loginService.login(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_LOGIN);
                });
    }

    @Test
    @DisplayName("이메일 없음과 비밀번호 불일치 모두 동일한 에러코드 반환")
    void login_fail_same_error_for_security() {
        // given - 이메일 없음
        LoginUseCase.Command wrongEmail = new LoginUseCase.Command("notexist@test.com", "password123");
        given(userJpaRepository.findByEmail(wrongEmail.email())).willReturn(Optional.empty());

        // given - 비밀번호 불일치
        LoginUseCase.Command wrongPassword = new LoginUseCase.Command("worker@test.com", "wrongPassword");
        given(userJpaRepository.findByEmail(wrongPassword.email())).willReturn(Optional.of(workerUser));
        given(passwordEncoder.matches(wrongPassword.password(), workerUser.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> loginService.login(wrongEmail))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_LOGIN));

        assertThatThrownBy(() -> loginService.login(wrongPassword))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_LOGIN));
    }
}
