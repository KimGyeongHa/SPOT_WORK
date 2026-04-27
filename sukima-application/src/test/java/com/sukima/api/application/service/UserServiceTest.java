package com.sukima.api.application.service;

import com.sukima.api.application.port.in.user.RegisterUserUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공")
    void register_success() {
        // given
        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "test@test.com", "password123", RoleType.WORKER
        );

        given(userJpaRepository.existsByEmail(command.email())).willReturn(false);
        given(passwordEncoder.encode(command.password())).willReturn("encoded_password");
        given(userJpaRepository.save(any(UserEntity.class))).willReturn(
                UserEntity.builder()
                        .id(1L)
                        .email(command.email())
                        .password("encoded_password")
                        .role(command.role())
                        .build()
        );

        // when
        Long userId = userService.register(command);

        // then
        assertThat(userId).isEqualTo(1L);
        verify(userJpaRepository).save(any(UserEntity.class));
        verify(passwordEncoder).encode(command.password());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_fail_duplicate_email() {
        // given
        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "duplicate@test.com", "password123", RoleType.WORKER
        );

        given(userJpaRepository.existsByEmail(command.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.register(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
                });

        verify(userJpaRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 암호화되어 저장된다")
    void register_password_encoded() {
        // given
        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "test@test.com", "plainPassword", RoleType.EMPLOYER
        );

        given(userJpaRepository.existsByEmail(command.email())).willReturn(false);
        given(passwordEncoder.encode("plainPassword")).willReturn("$2a$10$encodedPassword");
        given(userJpaRepository.save(any(UserEntity.class))).willAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            assertThat(saved.getPassword()).isEqualTo("$2a$10$encodedPassword");
            assertThat(saved.getPassword()).isNotEqualTo("plainPassword");
            return UserEntity.builder()
                    .id(1L)
                    .email(saved.getEmail())
                    .password(saved.getPassword())
                    .role(saved.getRole())
                    .build();
        });

        // when
        Long userId = userService.register(command);

        // then
        assertThat(userId).isNotNull();
        verify(passwordEncoder).encode("plainPassword");
    }

    @Test
    @DisplayName("WORKER 역할로 회원가입 성공")
    void register_success_worker_role() {
        // given
        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "worker@test.com", "password123", RoleType.WORKER
        );

        given(userJpaRepository.existsByEmail(command.email())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userJpaRepository.save(any(UserEntity.class))).willReturn(
                UserEntity.builder()
                        .id(1L)
                        .email(command.email())
                        .password("encoded")
                        .role(RoleType.WORKER)
                        .build()
        );

        // when
        Long userId = userService.register(command);

        // then
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("EMPLOYER 역할로 회원가입 성공")
    void register_success_employer_role() {
        // given
        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "employer@test.com", "password123", RoleType.EMPLOYER
        );

        given(userJpaRepository.existsByEmail(command.email())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userJpaRepository.save(any(UserEntity.class))).willReturn(
                UserEntity.builder()
                        .id(2L)
                        .email(command.email())
                        .password("encoded")
                        .role(RoleType.EMPLOYER)
                        .build()
        );

        // when
        Long userId = userService.register(command);

        // then
        assertThat(userId).isEqualTo(2L);
    }
}
