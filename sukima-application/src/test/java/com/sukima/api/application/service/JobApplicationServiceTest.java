package com.sukima.api.application.service;

import com.sukima.api.application.port.in.application.AcceptApplicationUseCase;
import com.sukima.api.application.port.in.application.ApplyJobUseCase;
import com.sukima.api.application.port.out.noshow.NoShowSchedulerPort;
import com.sukima.api.domain.application.type.ApplicationStatus;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.domain.job.type.JobStatus;
import com.sukima.api.infrastructure.persistence.entity.application.JobApplicationEntity;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.entity.match.MatchEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.JobApplicationJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.JobPostingJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.MatchJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobApplicationService 단위 테스트")
class JobApplicationServiceTest {

    @InjectMocks
    private JobApplicationService jobApplicationService;

    @Mock
    private JobApplicationJpaRepository jobApplicationJpaRepository;

    @Mock
    private JobPostingJpaRepository jobPostingJpaRepository;

    @Mock
    private WorkerJpaRepository workerJpaRepository;

    @Mock
    private MatchJpaRepository matchJpaRepository;

    @Mock
    private NoShowSchedulerPort noShowSchedulerPort;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private UserEntity workerUser;
    private WorkerEntity worker;
    private WorkerEntity penalizedWorker;
    private JobPostingEntity jobPosting;
    private JobApplicationEntity application;

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
                .rating(BigDecimal.ZERO)
                .build();

        jobPosting = JobPostingEntity.builder()
                .id(1L)
                .title("카페 알바")
                .location(geometryFactory.createPoint(new Coordinate(127.0276, 37.4979)))
                .address("서울시 강남구")
                .hourlyWage(10000)
                .capacity(3)
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(1).plusHours(4))
                .status(JobStatus.OPEN)
                .build();

        application = JobApplicationEntity.builder()
                .id(1L)
                .jobPosting(jobPosting)
                .worker(worker)
                .status(ApplicationStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();
    }

    // ── apply 테스트 ──────────────────────────────────────────

    @Test
    @DisplayName("공고 지원 성공")
    void apply_success() {
        // given
        ApplyJobUseCase.Command command = new ApplyJobUseCase.Command(1L, 1L);

        given(workerJpaRepository.findByUserId(command.userId())).willReturn(Optional.of(worker));
        given(jobApplicationJpaRepository.existsByJobPostingIdAndWorkerId(
                command.jobPostingId(), worker.getId())).willReturn(false);
        given(jobPostingJpaRepository.findById(command.jobPostingId())).willReturn(Optional.of(jobPosting));
        given(jobApplicationJpaRepository.save(any(JobApplicationEntity.class))).willReturn(application);

        // when
        Long applicationId = jobApplicationService.apply(command);

        // then
        assertThat(applicationId).isEqualTo(1L);
        verify(jobApplicationJpaRepository).save(any(JobApplicationEntity.class));
    }

    @Test
    @DisplayName("공고 지원 실패 - 구직자 프로필 없음")
    void apply_fail_worker_not_found() {
        // given
        ApplyJobUseCase.Command command = new ApplyJobUseCase.Command(1L, 999L);

        given(workerJpaRepository.findByUserId(command.userId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> jobApplicationService.apply(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.WORKER_NOT_FOUND));

        verify(jobApplicationJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("공고 지원 실패 - 패널티 상태의 구직자")
    void apply_fail_worker_penalized() {
        // given
        ApplyJobUseCase.Command command = new ApplyJobUseCase.Command(1L, 1L);

        // 3회 노쇼 → 패널티
        worker.increaseNoShow();
        worker.increaseNoShow();
        worker.increaseNoShow();

        given(workerJpaRepository.findByUserId(command.userId())).willReturn(Optional.of(worker));

        // when & then
        assertThatThrownBy(() -> jobApplicationService.apply(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.WORKER_PENALIZED));

        verify(jobApplicationJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("공고 지원 실패 - 중복 지원")
    void apply_fail_duplicate_application() {
        // given
        ApplyJobUseCase.Command command = new ApplyJobUseCase.Command(1L, 1L);

        given(workerJpaRepository.findByUserId(command.userId())).willReturn(Optional.of(worker));
        given(jobApplicationJpaRepository.existsByJobPostingIdAndWorkerId(
                command.jobPostingId(), worker.getId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> jobApplicationService.apply(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_APPLICATION));

        verify(jobApplicationJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("공고 지원 실패 - 존재하지 않는 공고")
    void apply_fail_job_posting_not_found() {
        // given
        ApplyJobUseCase.Command command = new ApplyJobUseCase.Command(999L, 1L);

        given(workerJpaRepository.findByUserId(command.userId())).willReturn(Optional.of(worker));
        given(jobApplicationJpaRepository.existsByJobPostingIdAndWorkerId(
                command.jobPostingId(), worker.getId())).willReturn(false);
        given(jobPostingJpaRepository.findById(command.jobPostingId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> jobApplicationService.apply(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.JOB_POSTING_NOT_FOUND));

        verify(jobApplicationJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("공고 지원 시 PENDING 상태로 저장된다")
    void apply_status_is_pending() {
        // given
        ApplyJobUseCase.Command command = new ApplyJobUseCase.Command(1L, 1L);

        given(workerJpaRepository.findByUserId(command.userId())).willReturn(Optional.of(worker));
        given(jobApplicationJpaRepository.existsByJobPostingIdAndWorkerId(
                command.jobPostingId(), worker.getId())).willReturn(false);
        given(jobPostingJpaRepository.findById(command.jobPostingId())).willReturn(Optional.of(jobPosting));
        given(jobApplicationJpaRepository.save(any(JobApplicationEntity.class))).willAnswer(invocation -> {
            JobApplicationEntity saved = invocation.getArgument(0);
            assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
            return application;
        });

        // when
        jobApplicationService.apply(command);
    }

    // ── accept 테스트 ──────────────────────────────────────────

    @Test
    @DisplayName("지원 수락 성공 - 매칭 생성 및 노쇼 스케줄 등록")
    void accept_success() {
        // given
        AcceptApplicationUseCase.Command command = new AcceptApplicationUseCase.Command(1L, 1L);

        MatchEntity match = MatchEntity.builder()
                .id(1L)
                .application(application)
                .jobPosting(jobPosting)
                .worker(worker)
                .status(com.sukima.api.domain.match.type.MatchStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .build();

        given(jobApplicationJpaRepository.findById(command.applicationId())).willReturn(Optional.of(application));
        given(jobApplicationJpaRepository.countByJobPostingIdAndStatus(
                jobPosting.getId(), ApplicationStatus.ACCEPTED.name())).willReturn(0);
        given(matchJpaRepository.save(any(MatchEntity.class))).willReturn(match);

        // when
        Long matchId = jobApplicationService.accept(command);

        // then
        assertThat(matchId).isEqualTo(1L);
        verify(matchJpaRepository).save(any(MatchEntity.class));
        // 노쇼 스케줄 등록 호출 확인
        verify(noShowSchedulerPort).schedule(match.getId(), jobPosting.getStartAt());
    }

    @Test
    @DisplayName("지원 수락 실패 - 존재하지 않는 지원")
    void accept_fail_application_not_found() {
        // given
        AcceptApplicationUseCase.Command command = new AcceptApplicationUseCase.Command(999L, 1L);

        given(jobApplicationJpaRepository.findById(command.applicationId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> jobApplicationService.accept(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND));

        verify(matchJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("지원 수락 실패 - 정원 초과")
    void accept_fail_capacity_exceeded() {
        // given
        AcceptApplicationUseCase.Command command = new AcceptApplicationUseCase.Command(1L, 1L);

        given(jobApplicationJpaRepository.findById(command.applicationId())).willReturn(Optional.of(application));
        // 정원 3명인데 이미 3명 수락됨
        given(jobApplicationJpaRepository.countByJobPostingIdAndStatus(
                jobPosting.getId(), ApplicationStatus.ACCEPTED.name())).willReturn(3);

        // when & then
        assertThatThrownBy(() -> jobApplicationService.accept(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CAPACITY_EXCEEDED));

        verify(matchJpaRepository, never()).save(any());
        verify(noShowSchedulerPort, never()).schedule(any(), any());
    }
}
