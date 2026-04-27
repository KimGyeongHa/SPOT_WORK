package com.sukima.api.application.service;

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
import com.sukima.api.infrastructure.persistence.entity.noshow.NoShowScheduleEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.MatchJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.NoShowScheduleJpaRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoShowService 단위 테스트")
class NoShowServiceTest {

    @InjectMocks
    private NoShowService noShowService;

    @Mock
    private MatchJpaRepository matchJpaRepository;

    @Mock
    private WorkLogJpaRepository workLogJpaRepository;

    @Mock
    private NoShowScheduleJpaRepository noShowScheduleJpaRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private WorkerEntity worker;
    private JobPostingEntity jobPosting;
    private MatchEntity confirmedMatch;
    private NoShowScheduleEntity noShowSchedule;

    @BeforeEach
    void setUp() {
        UserEntity workerUser = UserEntity.builder()
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
                .rating(BigDecimal.ZERO)
                .build();

        jobPosting = JobPostingEntity.builder()
                .id(1L)
                .employer(employer)
                .title("카페 알바")
                .location(geometryFactory.createPoint(new Coordinate(127.0276, 37.4979)))
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

        noShowSchedule = NoShowScheduleEntity.builder()
                .matchId(1L)
                .triggerAt(LocalDateTime.now().minusMinutes(5))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Test
    @DisplayName("노쇼 처리 성공 - Worker 패널티 부여 및 공고 OPEN 복구")
    void handle_success_noshow() {
        // given
        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(false);
        given(noShowScheduleJpaRepository.findByMatchId(1L)).willReturn(Optional.of(noShowSchedule));

        // when
        noShowService.handle(1L);

        // then
        // Worker 노쇼 카운트 증가 확인
        assertThat(worker.getNoShowCount()).isEqualTo(1);
        // 매칭 상태 CANCELLED 확인
        assertThat(confirmedMatch.getStatus()).isEqualTo(MatchStatus.CANCELLED);
        // 공고 상태 OPEN 복구 확인
        assertThat(jobPosting.getStatus()).isEqualTo(JobStatus.OPEN);
        // DB 처리 완료 마킹 확인
        assertThat(noShowSchedule.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("노쇼 처리 실패 - 존재하지 않는 매칭")
    void handle_fail_match_not_found() {
        // given
        given(matchJpaRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noShowService.handle(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        verify(workLogJpaRepository, never()).existsByMatchIdAndType(any(), any());
    }

    @Test
    @DisplayName("이미 처리된 매칭 - 노쇼 처리 skip")
    void handle_skip_already_processed() {
        // given - CANCELLED 상태 매칭
        MatchEntity cancelledMatch = MatchEntity.builder()
                .id(2L)
                .application(mock(JobApplicationEntity.class))
                .jobPosting(jobPosting)
                .worker(worker)
                .status(MatchStatus.CANCELLED)
                .confirmedAt(LocalDateTime.now().minusHours(2))
                .build();

        given(matchJpaRepository.findById(2L)).willReturn(Optional.of(cancelledMatch));

        // when
        noShowService.handle(2L);

        // then - 체크인 조회, 노쇼 처리 모두 호출 안 됨
        verify(workLogJpaRepository, never()).existsByMatchIdAndType(any(), any());
        assertThat(worker.getNoShowCount()).isEqualTo(0); // 카운트 증가 없음
    }

    @Test
    @DisplayName("체크인 완료된 매칭 - 노쇼 아님")
    void handle_skip_checked_in() {
        // given - 체크인 완료
        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(true);

        // when
        noShowService.handle(1L);

        // then - 노쇼 처리 안 됨
        assertThat(worker.getNoShowCount()).isEqualTo(0);
        assertThat(confirmedMatch.getStatus()).isEqualTo(MatchStatus.CONFIRMED); // 상태 변경 없음
        assertThat(jobPosting.getStatus()).isEqualTo(JobStatus.OPEN); // 공고 상태 변경 없음
    }

    @Test
    @DisplayName("노쇼 3회 누적 - 7일 패널티 부여")
    void handle_penalty_3_times() {
        // given - 이미 2회 노쇼
        worker.increaseNoShow();
        worker.increaseNoShow();

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(false);
        given(noShowScheduleJpaRepository.findByMatchId(1L)).willReturn(Optional.of(noShowSchedule));

        // when
        noShowService.handle(1L);

        // then - 3회째 노쇼 → 7일 패널티
        assertThat(worker.getNoShowCount()).isEqualTo(3);
        assertThat(worker.isPenalized()).isTrue();
        assertThat(worker.getPenaltyUntil()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("노쇼 5회 누적 - 30일 패널티 부여")
    void handle_penalty_5_times() {
        // given - 이미 4회 노쇼
        worker.increaseNoShow();
        worker.increaseNoShow();
        worker.increaseNoShow();
        worker.increaseNoShow();

        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(false);
        given(noShowScheduleJpaRepository.findByMatchId(1L)).willReturn(Optional.of(noShowSchedule));

        // when
        noShowService.handle(1L);

        // then - 5회째 노쇼 → 30일 패널티
        assertThat(worker.getNoShowCount()).isEqualTo(5);
        assertThat(worker.isPenalized()).isTrue();
        assertThat(worker.getPenaltyUntil()).isAfter(LocalDateTime.now().plusDays(29));
    }

    @Test
    @DisplayName("노쇼 스케줄 레코드 없어도 노쇼 처리는 정상 동작")
    void handle_success_without_schedule_record() {
        // given - DB 백업 레코드 없는 경우 (이미 처리됐거나 누락)
        given(matchJpaRepository.findById(1L)).willReturn(Optional.of(confirmedMatch));
        given(workLogJpaRepository.existsByMatchIdAndType(1L, WorkLogType.CHECK_IN)).willReturn(false);
        given(noShowScheduleJpaRepository.findByMatchId(1L)).willReturn(Optional.empty());

        // when
        noShowService.handle(1L);

        // then - 노쇼 처리는 정상 완료
        assertThat(worker.getNoShowCount()).isEqualTo(1);
        assertThat(confirmedMatch.getStatus()).isEqualTo(MatchStatus.CANCELLED);
    }
}
