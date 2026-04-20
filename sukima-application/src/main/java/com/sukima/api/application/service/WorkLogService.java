package com.sukima.api.application.service;

import com.sukima.api.application.port.in.worklog.CheckInUseCase;
import com.sukima.api.application.port.in.worklog.CheckOutUseCase;
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
@RequiredArgsConstructor
public class WorkLogService implements CheckInUseCase, CheckOutUseCase {

    private final WorkLogJpaRepository workLogJpaRepository;
    private final MatchJpaRepository matchJpaRepository;
    private final PaymentJpaRepository paymentJpaRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    // 허용 반경 (미터)
    private static final double ALLOWED_RADIUS_METERS = 100.0;

    @Override
    @Transactional
    public void checkIn(CheckInUseCase.Command command) {
        if (workLogJpaRepository.existsByMatchIdAndType(command.matchId(), WorkLogType.CHECK_IN)) {
            throw new IllegalStateException("이미 체크인되었습니다.");
        }

        MatchEntity match = matchJpaRepository.findById(command.matchId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매칭입니다."));

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
    }

    @Override
    @Transactional
    public void checkOut(CheckOutUseCase.Command command) {
        if (!workLogJpaRepository.existsByMatchIdAndType(command.matchId(), WorkLogType.CHECK_IN)) {
            throw new IllegalStateException("체크인 후 체크아웃할 수 있습니다.");
        }

        if (workLogJpaRepository.existsByMatchIdAndType(command.matchId(), WorkLogType.CHECK_OUT)) {
            throw new IllegalStateException("이미 체크아웃되었습니다.");
        }

        MatchEntity match = matchJpaRepository.findById(command.matchId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매칭입니다."));

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

        // 정산 처리
        processPayment(match, checkOut.getScannedAt());
    }

    private void validateLocation(MatchEntity match, double lat, double lng) {
        Point jobLocation = match.getJobPosting().getLocation();
        double jobLat = jobLocation.getY();
        double jobLng = jobLocation.getX();

        double distance = haversine(jobLat, jobLng, lat, lng);
        if (distance > ALLOWED_RADIUS_METERS) {
            throw new IllegalStateException("근무지에서 너무 멀리 떨어져 있습니다. (허용 반경: " + ALLOWED_RADIUS_METERS + "m)");
        }
    }

    private void processPayment(MatchEntity match, LocalDateTime checkOutTime) {
        WorkLogEntity checkIn = workLogJpaRepository
                .findByMatchIdAndType(match.getId(), WorkLogType.CHECK_IN)
                .orElseThrow(() -> new IllegalStateException("체크인 기록이 없습니다."));

        long minutes = Duration.between(checkIn.getScannedAt(), checkOutTime).toMinutes();
        int amount = (int) (match.getJobPosting().getHourlyWage() * minutes / 60.0);

        PaymentEntity payment = PaymentEntity.builder()
                .match(match)
                .worker(match.getWorker())
                .amount(amount)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        paymentJpaRepository.save(payment);
    }

    // Haversine 공식으로 두 좌표 간 거리(미터) 계산
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
