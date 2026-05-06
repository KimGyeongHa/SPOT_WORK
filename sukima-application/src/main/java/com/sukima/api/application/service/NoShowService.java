package com.sukima.api.application.service;

import com.sukima.api.application.port.in.noshow.HandleNoShowUseCase;
import com.sukima.api.application.port.out.notification.NotificationPort;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.job.type.JobStatus;
import com.sukima.api.domain.match.type.MatchStatus;
import com.sukima.api.domain.worklog.type.WorkLogType;
import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import com.sukima.api.infrastructure.persistence.entity.noshow.NoShowScheduleEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.MatchJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.NoShowScheduleJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoShowService implements HandleNoShowUseCase {

    private final MatchJpaRepository matchJpaRepository;
    private final WorkLogJpaRepository workLogJpaRepository;
    private final NoShowScheduleJpaRepository noShowScheduleJpaRepository;
    private final NotificationPort notificationPort;

    @Override
    @Transactional
    public void handle(Long matchId) {
        MatchEntity match = matchJpaRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 이미 처리된 매칭이면 skip
        if (match.getStatus() != MatchStatus.CONFIRMED) {
            log.info("이미 처리된 매칭, 노쇼 체크 skip: matchId={}, status={}",
                    matchId, match.getStatus());
            return;
        }

        // 체크인 여부 확인
        boolean checkedIn = workLogJpaRepository.existsByMatchIdAndType(matchId, WorkLogType.CHECK_IN);
        if (checkedIn) {
            log.info("체크인 완료된 매칭, 노쇼 아님: matchId={}", matchId);
            return;
        }

        // 노쇼 처리
        WorkerEntity worker = match.getWorker();
        worker.increaseNoShow();
        log.warn("노쇼 처리: matchId={}, workerId={}, noShowCount={}, penaltyUntil={}",
                matchId, worker.getId(), worker.getNoShowCount(), worker.getPenaltyUntil());

        // 매칭 취소
        match.cancelByNoShow();

        // 공고 다시 OPEN으로
        match.getJobPosting().reopen();

        // Employer에게 노쇼 알림 (SSE)
        notificationPort.notifyNoShow(
                match.getJobPosting().getEmployer().getUser().getId(),
                matchId,
                worker.getName()
        );

        // DB 처리 완료 마킹
        noShowScheduleJpaRepository.findByMatchId(matchId)
                .ifPresent(NoShowScheduleEntity::markProcessed);
    }
}
