package com.sukima.api.application.service;

import com.sukima.api.application.port.in.qr.IssueQrTokenUseCase;
import com.sukima.api.application.port.out.qr.QrTokenPort;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.match.type.MatchStatus;
import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import com.sukima.api.infrastructure.persistence.repository.MatchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QrTokenService implements IssueQrTokenUseCase {

    private final MatchJpaRepository matchJpaRepository;
    private final QrTokenPort qrTokenPort;

    @Override
    @Transactional
    public Result issue(Command command) {
        MatchEntity match = matchJpaRepository.findById(command.matchId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 본인의 매칭이 맞는지 확인
        if (!match.getWorker().getUser().getId().equals(command.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 매칭이 CONFIRMED 상태에서만 QR 발급 가능
        if (match.getStatus() != MatchStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.INVALID_MATCH_STATUS);
        }

        // 재발급 요청이면 버전 증가 → 이전 토큰 무효화
        if (command.reissue()) {
            match.increaseQrVersion();
        }

        String token = qrTokenPort.generate(
                match.getId(),
                match.getWorker().getId(),
                match.getQrVersion(),
                match.getJobPosting().getEndAt()
        );

        return new Result(token, match.getJobPosting().getEndAt().plusMinutes(30));
    }
}
