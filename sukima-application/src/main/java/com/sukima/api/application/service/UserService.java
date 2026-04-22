package com.sukima.api.application.service;

import com.sukima.api.application.port.in.user.RegisterUserUseCase;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService implements RegisterUserUseCase {

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Long register(Command command) {
        if (userJpaRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        UserEntity entity = UserEntity.builder()
                .email(command.email())
                .password(passwordEncoder.encode(command.password()))
                .role(command.role())
                .build();

        return userJpaRepository.save(entity).getId();
    }
}
