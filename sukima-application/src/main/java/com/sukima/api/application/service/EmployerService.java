package com.sukima.api.application.service;

import com.sukima.api.application.port.in.employer.RegisterEmployerUseCase;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.repository.EmployerJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EmployerService implements RegisterEmployerUseCase {

    private final EmployerJpaRepository employerJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional
    public Long register(Command command) {
        if (employerJpaRepository.existsByUserId(command.userId())) {
            throw new IllegalArgumentException("이미 등록된 구인자 프로필입니다.");
        }

        UserEntity userEntity = userJpaRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (userEntity.getRole() != RoleType.EMPLOYER) {
            throw new IllegalArgumentException("EMPLOYER 역할의 사용자만 고용주 프로필을 등록할 수 있습니다.");
        }

        EmployerEntity entity = EmployerEntity.builder()
                .user(userEntity)
                .name(command.name())
                .phone(command.phone())
                .companyName(command.companyName())
                .rating(BigDecimal.ZERO)
                .build();

        return employerJpaRepository.save(entity).getId();
    }
}
