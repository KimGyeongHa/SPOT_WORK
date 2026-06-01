package com.sukima.api.application.service;

import com.sukima.api.application.port.in.worklog.CheckInUseCase;
import com.sukima.api.application.port.in.worklog.CheckOutUseCase;
import com.sukima.api.application.port.out.noshow.NoShowSchedulerPort;
import com.sukima.api.application.port.out.qr.QrTokenPort;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.match.type.MatchStatus;
import com.sukima.api.domain.payment.type.PaymentStatus;
import com.sukima.api.domain.worklog.type.WorkLogType;
import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import com.sukima.api.infrastructure.persistence.entity.payment.PaymentEntity;
import com.sukima.api.infrastructure.persistence.entity.worklog.WorkLogEntity;
import com.sukima.api.infrastructure.persistence.repository.MatchJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.PaymentJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkLogService implements CheckInUseCase, CheckOutUseCase {

    private final WorkLogJpaRepository workLogJpaRepository;
    private final MatchJpaRepository matchJpaRepository;
    private final PaymentJpaRepository paymentJpaRepository;
    private final NoShowSchedulerPort noShowSchedulerPort;
    private final QrTokenPort qrTokenPort;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private static final double ALLOWED_RADIUS_METERS = 100.0;

    @Override
    @Transactional
    public void checkIn(CheckInUseCase.Command command) {
        MatchEntity match = matchJpaRepository.findById(command.matchId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 본인의 매칭이 맞는지 확인
        validateOwner(match, command.userId());

        // QR 토큰 검증
        validateQrToken(command.qrToken(), match);

        // 매칭이 CONFIRMED 상태인지 확인
        if (match.getStatus() != MatchStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.INVALID_MATCH_STATUS);
        }

        if (workLogJpaRepository.existsByMatchIdAndType(command.matchId(), WorkLogType.CHECK_IN)) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        }

        validateLocation(match, command.latitude(), command.longitude());

        Point point = geometryFactory.createPoint(
                new Coordinate(command.longitude(), command.latitude())
        );

        WorkLogEntity entity = WorkLogEntity.builder()
                .match(match)
                .type(WorkLogType.CHECK_IN)
                .location(point)
                .scannedAt(LocalDateTime.now())
                .build();

        workLogJpaRepository.save(entity);

        // 정상 체크인 → 노쇼 체크 예약 취소
        noShowSchedulerPort.cancel(command.matchId());
    }

    @Override
    @Transactional
    public void checkOut(CheckOutUseCase.Command command) {
        MatchEntity match = matchJpaRepository.findById(command.matchId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 본인의 매칭이 맞는지 확인
        validateOwner(match, command.userId());

        // QR 토큰 검증
        validateQrToken(command.qrToken(), match);

        if (!workLogJpaRepository.existsByMatchIdAndType(command.matchId(), WorkLogType.CHECK_IN)) {
            throw new BusinessException(ErrorCode.CHECK_IN_REQUIRED);
        }

        if (workLogJpaRepository.existsByMatchIdAndType(command.matchId(), WorkLogType.CHECK_OUT)) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_OUT);
        }

        validateLocation(match, command.latitude(), command.longitude());

        Point point = geometryFactory.createPoint(
                new Coordinate(command.longitude(), command.latitude())
        );

        WorkLogEntity checkOut = WorkLogEntity.builder()
                .match(match)
                .type(WorkLogType.CHECK_OUT)
                .location(point)
                .scannedAt(LocalDateTime.now())
                .build();

        workLogJpaRepository.save(checkOut);

        processPayment(match, checkOut.getScannedAt());

        // 매칭 상태를 COMPLETED로 변경
        match.complete();
    }

    /**
     * 매칭의 Worker가 요청자(userId)와 일치하는지 검증
     */
    private void validateOwner(MatchEntity match, Long userId) {
        if (!match.getWorker().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * QR 토큰의 matchId, workerId, version이 매칭과 일치하는지 검증
     */
    private void validateQrToken(String qrToken, MatchEntity match) {
        QrTokenPort.QrTokenInfo info = qrTokenPort.parse(qrToken);

        if (!info.matchId().equals(match.getId())
                || !info.workerId().equals(match.getWorker().getId())) {
            throw new BusinessException(ErrorCode.INVALID_QR_TOKEN);
        }

        // 최신 버전이 아니면 거부 (재발급된 후 이전 토큰 사용 불가)
        if (!info.version().equals(match.getQrVersion())) {
            throw new BusinessException(ErrorCode.QR_VERSION_MISMATCH);
        }
    }

    private void validateLocation(MatchEntity match, double lat, double lng) {
        Point jobLocation = match.getJobPosting().getLocation();
        double jobLat = jobLocation.getY();
        double jobLng = jobLocation.getX();

        double distance = haversine(jobLat, jobLng, lat, lng);
        if (distance > ALLOWED_RADIUS_METERS) {
            throw new BusinessException(ErrorCode.OUT_OF_RANGE);
        }
    }

    // 중복 정산 방지 + PENDING 상태로 생성 (외부 결제 API 콜백 시 COMPLETED 전환)
    private void processPayment(MatchEntity match, LocalDateTime checkOutTime) {
        if (paymentJpaRepository.existsByMatchId(match.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
        }

        WorkLogEntity checkIn = workLogJpaRepository
                .findByMatchIdAndType(match.getId(), WorkLogType.CHECK_IN)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECK_IN_REQUIRED));

        long minutes = Duration.between(checkIn.getScannedAt(), checkOutTime).toMinutes();
        int amount = (int) (match.getJobPosting().getHourlyWage() * minutes / 60.0);

        paymentJpaRepository.save(PaymentEntity.builder()
                .match(match)
                .worker(match.getWorker())
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
