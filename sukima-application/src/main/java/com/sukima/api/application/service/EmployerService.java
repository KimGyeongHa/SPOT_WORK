package com.sukima.api.application.service;

import com.sukima.api.application.port.in.employer.RegisterEmployerUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.repository.EmployerJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EmployerService implements RegisterEmployerUseCase {

    private final EmployerJpaRepository employerJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional
    public Long register(Command command) {
        if (employerJpaRepository.existsByUserId(command.userId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYER_PROFILE);
        }

        UserEntity userEntity = userJpaRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (userEntity.getRole() != RoleType.EMPLOYER) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }

        return employerJpaRepository.save(EmployerEntity.builder()
                .user(userEntity)
                .name(command.name())
                .phone(command.phone())
                .companyName(command.companyName())
                .build()).getId();
    }
}
