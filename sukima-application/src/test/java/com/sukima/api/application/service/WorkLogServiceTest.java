package com.sukima.api.application.service;

import com.sukima.api.application.port.in.worklog.CheckInUseCase;
import com.sukima.api.application.port.in.worklog.CheckOutUseCase;
import com.sukima.api.application.port.out.noshow.NoShowSchedulerPort;
import com.sukima.api.application.port.out.qr.QrTokenPort;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.domain.job.type.JobStatus;
import com.sukima.api.domain.match.type.MatchStatus;
import com.sukima.api.domain.worklog.type.WorkLogType;
import com.sukima.api.infrastructure.persistence.entity.application.JobApplicationEntity;
import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.entity.worklog.WorkLogEntity;
import com.sukima.api.infrastructure.persistence.repository.MatchJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.PaymentJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkLogJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkLogService 단위 테스트")
class WorkLogServiceTest {

    @InjectMocks
    private WorkLogService workLogService;

    @Mock
    private WorkLogJpaRepository workLogJpaRepository;

    @Mock
    private MatchJpaRepository matchJpaRepository;

    @Mock
    private PaymentJpaRepository paymentJpaRepository;

    @Mock
    private NoShowSchedulerPort noShowSchedulerPort;

    @Mock
    private QrTokenPort qrTokenPort;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private UserEntity workerUser;
    private WorkerEntity worker;
    private JobPostingEntity jobPosting;
    private MatchEntity confirmedMatch;
    private QrTokenPort.QrTokenInfo validQrInfo;

    // 근무지 좌표 (서울 강남)
    private static final double JOB_LAT = 37.4979;
    private static final double JOB_LNG = 127.0276;

    // 근무지 반경 50m 이내 좌표 (유효)
    private static final double NEAR_LAT = 37.4980;
    private static final double NEAR_LNG = 127.0277;

    // 근무지 반경 500m 초과 좌표 (무효)
    private static final double FAR_LAT = 37.5050;
    private static final double FAR_LNG = 127.0500;

    @BeforeEach
    void setUp() {
        workerUser = UserEntity.builder()
                .id(1L)
                .email("worker@test.com")
                .password("encoded")
                .role(RoleType.WORKER)
                .build();

        worker = WorkerEntity.builder()
                .id(1L)
                .user(workerUser)
                .name("홍길동")
                .phone("010-1234-5678")
                .build();

        UserEntity employerUser = UserEntity.builder()
                .id(2L)
                .email("employer@test.com")
                .password("encoded")
                .role(RoleType.EMPLOYER)
                .build();

        EmployerEntity employer = EmployerEntity.builder()
                .id(1L)
                .user(employerUser)
                .name("김사장")
                .phone("010-9999-8888")
                .companyName("스키마카페")
                .build();

        jobPosting = JobPostingEntity.builder()
                .id(1L)
                .employer(employer)
                .title("카페 알바")
                .location(geometryFactory.createPoint(new Coordinate(JOB_LNG, JOB_LAT)))
                .address("서울시 강남구")
                .hourlyWage(10000)
                .capacity(3)
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(3))
                .status(JobStatus.OPEN)
                .build();

        confirmedMatch = MatchEntity.builder()
                .id(1L)
                .application(mock(JobApplicationEntity.class))
                .jobPosting(jobPosting)
                .worker(worker)
                .status(MatchStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now().minusHours(2))
                .build();

        validQrInfo = new QrTokenPort.QrTokenInfo(1L, 1L, 0L);
    }

    // ── checkIn 테스트 ──────────────────────────────────────────

    @Test
    @DisplayName("체크인 성공")
    void checkIn_success() {
        // given
        CheckInUseCase.Command command = new CheckInUseCase.Command(
                1L, 1L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(validQrInfo);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(false);

        // when
        workLogService.checkIn(command);

        // then
        verify(workLogJpaRepository).save(any(WorkLogEntity.class));
        verify(noShowSchedulerPort).cancel(1L);
    }

    @Test
    @DisplayName("체크인 성공 - 노쇼 스케줄 취소 호출 확인")
    void checkIn_cancels_noshow_schedule() {
        // given
        CheckInUseCase.Command command = new CheckInUseCase.Command(
                1L, 1L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(validQrInfo);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(false);

        // when
        workLogService.checkIn(command);

        // then - 노쇼 스케줄 반드시 취소
        verify(noShowSchedulerPort).cancel(1L);
    }

    @Test
    @DisplayName("체크인 실패 - 존재하지 않는 매칭")
    void checkIn_fail_match_not_found() {
        // given
        CheckInUseCase.Command command = new CheckInUseCase.Command(
                999L, 1L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workLogService.checkIn(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        verify(workLogJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("체크인 실패 - 본인의 매칭이 아님")
    void checkIn_fail_forbidden() {
        // given - 다른 userId
        CheckInUseCase.Command command = new CheckInUseCase.Command(
                1L, 999L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));

        // when & then
        assertThatThrownBy(() -> workLogService.checkIn(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));

        verify(workLogJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("체크인 실패 - 유효하지 않은 QR 토큰")
    void checkIn_fail_invalid_qr() {
        // given
        CheckInUseCase.Command command = new CheckInUseCase.Command(
                1L, 1L, "invalid.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("invalid.qr.token"))
                .willThrow(new BusinessException(ErrorCode.INVALID_QR_TOKEN));

        // when & then
        assertThatThrownBy(() -> workLogService.checkIn(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_QR_TOKEN));

        verify(workLogJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("체크인 실패 - 구버전 QR 토큰 (재발급 후 이전 토큰)")
    void checkIn_fail_qr_version_mismatch() {
        // given - 버전이 1 증가된 매칭 (재발급 됨)
        confirmedMatch.increaseQrVersion(); // version = 1

        // 구버전 토큰 (version = 0)
        QrTokenPort.QrTokenInfo oldVersionInfo = new QrTokenPort.QrTokenInfo(1L, 1L, 0L);

        CheckInUseCase.Command command = new CheckInUseCase.Command(
                1L, 1L, "old.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("old.qr.token")).willReturn(oldVersionInfo);

        // when & then
        assertThatThrownBy(() -> workLogService.checkIn(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.QR_VERSION_MISMATCH));

        verify(workLogJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("체크인 실패 - 이미 체크인됨")
    void checkIn_fail_already_checked_in() {
        // given
        CheckInUseCase.Command command = new CheckInUseCase.Command(
                1L, 1L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(validQrInfo);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> workLogService.checkIn(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_CHECKED_IN));
    }

    @Test
    @DisplayName("체크인 실패 - 근무지 반경 벗어남")
    void checkIn_fail_out_of_range() {
        // given - 멀리 떨어진 좌표
        CheckInUseCase.Command command = new CheckInUseCase.Command(
                1L, 1L, "valid.qr.token", FAR_LAT, FAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(validQrInfo);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> workLogService.checkIn(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.OUT_OF_RANGE));

        verify(workLogJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("체크인 실패 - CONFIRMED 아닌 매칭 상태")
    void checkIn_fail_invalid_match_status() {
        // given - COMPLETED 매칭
        MatchEntity completedMatch = MatchEntity.builder()
                .id(4L)
                .application(mock(JobApplicationEntity.class))
                .jobPosting(jobPosting)
                .worker(worker)
                .status(MatchStatus.COMPLETED)
                .confirmedAt(LocalDateTime.now().minusHours(5))
                .build();
        completedMatch.complete();

        CheckInUseCase.Command command = new CheckInUseCase.Command(
                4L, 1L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(4L)).willReturn(Optional.of(completedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(
                new QrTokenPort.QrTokenInfo(4L, 1L, 0L)
        );

        // when & then
        assertThatThrownBy(() -> workLogService.checkIn(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_MATCH_STATUS));
    }

    // ── checkOut 테스트 ──────────────────────────────────────────

    @Test
    @DisplayName("체크아웃 성공 - 정산 생성 및 매칭 COMPLETED")
    void checkOut_success() {
        // given
        CheckOutUseCase.Command command = new CheckOutUseCase.Command(
                1L, 1L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        WorkLogEntity checkInLog = WorkLogEntity.builder()
                .id(1L)
                .match(confirmedMatch)
                .type(WorkLogType.CHECK_IN)
                .location(geometryFactory.createPoint(new Coordinate(JOB_LNG, JOB_LAT)))
                .scannedAt(LocalDateTime.now().minusHours(2))
                .build();

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(validQrInfo);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(true);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_OUT)).willReturn(false);
        given(workLogJpaRepository.findByMatchIdAndType(1L, WorkLogType.CHECK_IN))
                .willReturn(Optional.of(checkInLog));

        // when
        workLogService.checkOut(command);

        // then
        verify(workLogJpaRepository).save(any(WorkLogEntity.class));
        verify(paymentJpaRepository).save(any());
        assertThat(confirmedMatch.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(confirmedMatch.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("체크아웃 실패 - 체크인 안 됨")
    void checkOut_fail_check_in_required() {
        // given
        CheckOutUseCase.Command command = new CheckOutUseCase.Command(
                1L, 1L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(validQrInfo);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> workLogService.checkOut(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHECK_IN_REQUIRED));

        verify(paymentJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("체크아웃 실패 - 이미 체크아웃됨")
    void checkOut_fail_already_checked_out() {
        // given
        CheckOutUseCase.Command command = new CheckOutUseCase.Command(
                1L, 1L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(validQrInfo);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(true);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_OUT)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> workLogService.checkOut(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_CHECKED_OUT));

        verify(paymentJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("체크아웃 실패 - 근무지 반경 벗어남")
    void checkOut_fail_out_of_range() {
        // given
        CheckOutUseCase.Command command = new CheckOutUseCase.Command(
                1L, 1L, "valid.qr.token", FAR_LAT, FAR_LNG
        );

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(validQrInfo);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(true);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_OUT)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> workLogService.checkOut(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.OUT_OF_RANGE));

        verify(paymentJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("체크아웃 시 정산 금액 = 시급 × 실근무시간")
    void checkOut_payment_amount_correct() {
        // given - 2시간 근무 (시급 10000 → 20000원)
        CheckOutUseCase.Command command = new CheckOutUseCase.Command(
                1L, 1L, "valid.qr.token", NEAR_LAT, NEAR_LNG
        );

        WorkLogEntity checkInLog = WorkLogEntity.builder()
                .id(1L)
                .match(confirmedMatch)
                .type(WorkLogType.CHECK_IN)
                .location(geometryFactory.createPoint(new Coordinate(JOB_LNG, JOB_LAT)))
                .scannedAt(LocalDateTime.now().minusHours(2)) // 2시간 전 체크인
                .build();

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.parse("valid.qr.token")).willReturn(validQrInfo);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(true);
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_OUT)).willReturn(false);
        given(workLogJpaRepository.findByMatchIdAndType(1L, WorkLogType.CHECK_IN))
                .willReturn(Optional.of(checkInLog));
        given(paymentJpaRepository.save(any())).willAnswer(invocation -> {
            var payment = invocation.getArgument(0,
                    com.sukima.api.infrastructure.persistence.entity.payment.PaymentEntity.class);
            // 2시간 × 10000원 = 20000원
            assertThat(payment.getAmount()).isBetween(19000, 21000); // 오차 허용
            return payment;
        });

        // when
        workLogService.checkOut(command);
    }
}
