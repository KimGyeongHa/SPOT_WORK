package com.sukima.api.application.service;

import com.sukima.api.application.port.in.worker.RegisterWorkerUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.UserJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkerService implements RegisterWorkerUseCase {

    private final WorkerJpaRepository workerJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional
    public Long register(Command command) {
        if (workerJpaRepository.existsByUserId(command.userId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_WORKER_PROFILE);
        }

        UserEntity userEntity = userJpaRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (userEntity.getRole() != RoleType.WORKER) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }

        return workerJpaRepository.save(WorkerEntity.builder()
                .user(userEntity)
                .name(command.name())
                .phone(command.phone())
                .build()).getId();
    }
}
