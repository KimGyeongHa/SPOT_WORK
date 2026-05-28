package com.sukima.api.application.service;

import com.sukima.api.application.port.in.worker.UpdateNotificationSettingUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkerNotificationService implements UpdateNotificationSettingUseCase {

    private final WorkerJpaRepository workerJpaRepository;

    @Override
    @Transactional
    public void update(Command command) {
        var worker = workerJpaRepository.findByUserId(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKER_NOT_FOUND));

        worker.updateNotificationSetting(
                command.enabled(),
                command.lat(),
                command.lng(),
                command.radiusMeters()
        );
    }
}
