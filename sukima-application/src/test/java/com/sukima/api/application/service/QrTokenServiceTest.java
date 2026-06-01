package com.sukima.api.application.service;

import com.sukima.api.application.port.in.qr.IssueQrTokenUseCase;
import com.sukima.api.application.port.out.qr.QrTokenPort;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.domain.job.type.JobStatus;
import com.sukima.api.domain.match.type.MatchStatus;
import com.sukima.api.infrastructure.persistence.entity.application.JobApplicationEntity;
import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.MatchJpaRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrTokenService 단위 테스트")
class QrTokenServiceTest {

    @InjectMocks
    private QrTokenService qrTokenService;

    @Mock
    private MatchJpaRepository matchJpaRepository;

    @Mock
    private QrTokenPort qrTokenPort;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private UserEntity workerUser;
    private WorkerEntity worker;
    private JobPostingEntity jobPosting;
    private MatchEntity confirmedMatch;
    private MatchEntity cancelledMatch;
    private MatchEntity completedMatch;

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
                .location(geometryFactory.createPoint(new Coordinate(127.0276, 37.4979)))
                .address("서울시 강남구")
                .hourlyWage(10000)
                .capacity(3)
                .startAt(LocalDateTime.now().plusHours(1))
                .endAt(LocalDateTime.now().plusHours(5))
                .status(JobStatus.OPEN)
                .build();

        confirmedMatch = MatchEntity.builder()
                .id(1L)
                .application(mock(JobApplicationEntity.class))
                .jobPosting(jobPosting)
                .worker(worker)
                .status(MatchStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now().minusHours(1))
                .build();

        cancelledMatch = MatchEntity.builder()
                .id(2L)
                .application(mock(JobApplicationEntity.class))
                .jobPosting(jobPosting)
                .worker(worker)
                .status(MatchStatus.CANCELLED)
                .confirmedAt(LocalDateTime.now().minusHours(1))
                .build();

        completedMatch = MatchEntity.builder()
                .id(3L)
                .application(mock(JobApplicationEntity.class))
                .jobPosting(jobPosting)
                .worker(worker)
                .status(MatchStatus.COMPLETED)
                .confirmedAt(LocalDateTime.now().minusHours(5))
                .build();
    }

    // ── QR 발급 테스트 ──────────────────────────────────────────

    @Test
    @DisplayName("QR 토큰 발급 성공")
    void issue_success() {
        // given
        IssueQrTokenUseCase.Command command = new IssueQrTokenUseCase.Command(1L, 1L, false);

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.generate(
                eq(confirmedMatch.getId()),
                eq(worker.getId()),
                eq(confirmedMatch.getQrVersion()),
                any(LocalDateTime.class)
        )).willReturn("mock.qr.token");

        // when
        IssueQrTokenUseCase.Result result = qrTokenService.issue(command);

        // then
        assertThat(result.qrToken()).isEqualTo("mock.qr.token");
        assertThat(result.expiresAt()).isAfter(LocalDateTime.now());
        verify(qrTokenPort).generate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("QR 토큰 발급 실패 - 존재하지 않는 매칭")
    void issue_fail_match_not_found() {
        // given
        IssueQrTokenUseCase.Command command = new IssueQrTokenUseCase.Command(999L, 1L, false);

        given(matchJpaRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> qrTokenService.issue(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        verify(qrTokenPort, never()).generate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("QR 토큰 발급 실패 - 본인의 매칭이 아님")
    void issue_fail_forbidden() {
        // given - 다른 userId로 요청
        IssueQrTokenUseCase.Command command = new IssueQrTokenUseCase.Command(1L, 999L, false);

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));

        // when & then
        assertThatThrownBy(() -> qrTokenService.issue(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));

        verify(qrTokenPort, never()).generate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("QR 토큰 발급 실패 - CANCELLED 매칭")
    void issue_fail_cancelled_match() {
        // given
        IssueQrTokenUseCase.Command command = new IssueQrTokenUseCase.Command(2L, 1L, false);

        given(matchJpaRepository.findById(2L)).willReturn(Optional.of(cancelledMatch));

        // when & then
        assertThatThrownBy(() -> qrTokenService.issue(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_MATCH_STATUS));

        verify(qrTokenPort, never()).generate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("QR 토큰 발급 실패 - COMPLETED 매칭")
    void issue_fail_completed_match() {
        // given
        IssueQrTokenUseCase.Command command = new IssueQrTokenUseCase.Command(3L, 1L, false);

        given(matchJpaRepository.findById(3L)).willReturn(Optional.of(completedMatch));

        // when & then
        assertThatThrownBy(() -> qrTokenService.issue(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_MATCH_STATUS));
    }

    // ── QR 재발급 테스트 ──────────────────────────────────────────

    @Test
    @DisplayName("QR 토큰 재발급 성공 - 버전 증가")
    void reissue_success_version_increased() {
        // given
        IssueQrTokenUseCase.Command command = new IssueQrTokenUseCase.Command(1L, 1L, true);

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.generate(any(), any(), any(), any())).willReturn("new.qr.token");

        Long versionBefore = confirmedMatch.getQrVersion();

        // when
        IssueQrTokenUseCase.Result result = qrTokenService.issue(command);

        // then - 버전이 1 증가했는지 확인
        assertThat(confirmedMatch.getQrVersion()).isEqualTo(versionBefore + 1);
        assertThat(result.qrToken()).isEqualTo("new.qr.token");
    }

    @Test
    @DisplayName("일반 발급 시 버전 증가 없음")
    void issue_no_version_increase() {
        // given
        IssueQrTokenUseCase.Command command = new IssueQrTokenUseCase.Command(1L, 1L, false);

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.generate(any(), any(), any(), any())).willReturn("qr.token");

        Long versionBefore = confirmedMatch.getQrVersion();

        // when
        qrTokenService.issue(command);

        // then - 버전 변화 없음
        assertThat(confirmedMatch.getQrVersion()).isEqualTo(versionBefore);
    }

    @Test
    @DisplayName("만료 시각은 근무 종료 + 30분")
    void issue_expires_at_correct() {
        // given
        IssueQrTokenUseCase.Command command = new IssueQrTokenUseCase.Command(1L, 1L, false);

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(qrTokenPort.generate(any(), any(), any(), any())).willReturn("qr.token");

        LocalDateTime expectedExpiresAt = jobPosting.getEndAt().plusMinutes(30);

        // when
        IssueQrTokenUseCase.Result result = qrTokenService.issue(command);

        // then
        assertThat(result.expiresAt()).isEqualTo(expectedExpiresAt);
    }
}
