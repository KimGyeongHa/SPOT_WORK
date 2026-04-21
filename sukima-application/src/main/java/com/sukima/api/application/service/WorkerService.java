package com.sukima.api.application.service;

import com.sukima.api.application.port.in.worker.RegisterWorkerUseCase;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.UserJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WorkerService implements RegisterWorkerUseCase {

    private final WorkerJpaRepository workerJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional
    public Long register(Command command) {
        if (workerJpaRepository.existsByUserId(command.userId())) {
            throw new IllegalArgumentException("이미 등록된 구직자 프로필입니다.");
        }

        UserEntity userEntity = userJpaRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (userEntity.getRole() != RoleType.WORKER) {
            throw new IllegalArgumentException("WORKER 역할의 사용자만 구직자 프로필을 등록할 수 있습니다.");
        }

        WorkerEntity entity = WorkerEntity.builder()
                .user(userEntity)
                .name(command.name())
                .phone(command.phone())
                .rating(BigDecimal.ZERO)
                .build();

        return workerJpaRepository.save(entity).getId();
    }
}
