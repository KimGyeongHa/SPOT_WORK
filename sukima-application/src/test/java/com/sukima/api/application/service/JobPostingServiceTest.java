package com.sukima.api.application.service;

import com.sukima.api.application.port.in.job.CreateJobPostingUseCase;
import com.sukima.api.application.port.in.job.GetNearbyJobPostingsUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.domain.common.type.RoleType;
import com.sukima.api.domain.job.JobPosting;
import com.sukima.api.domain.job.type.JobStatus;
import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import com.sukima.api.infrastructure.persistence.repository.EmployerJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.JobPostingJpaRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobPostingService 단위 테스트")
class JobPostingServiceTest {

    @InjectMocks
    private JobPostingService jobPostingService;

    @Mock
    private JobPostingJpaRepository jobPostingJpaRepository;

    @Mock
    private EmployerJpaRepository employerJpaRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private UserEntity employerUser;
    private EmployerEntity employer;
    private EmployerEntity penalizedEmployer;
    private CreateJobPostingUseCase.Command command;

    @BeforeEach
    void setUp() {
        employerUser = UserEntity.builder()
                .id(1L)
                .email("employer@test.com")
                .password("encoded")
                .role(RoleType.EMPLOYER)
                .build();

        employer = EmployerEntity.builder()
                .id(1L)
                .user(employerUser)
                .name("홍길동")
                .phone("010-1234-5678")
                .companyName("스키마카페")
                .build();

        command = new CreateJobPostingUseCase.Command(
                1L,
                "카페 알바 구합니다",
                "주말 오전 카페 알바",
                37.4979,
                127.0276,
                "서울시 강남구 역삼동",
                10000,
                3,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(4)
        );
    }

    @Test
    @DisplayName("공고 등록 성공")
    void create_success() {
        // given
        given(employerJpaRepository.findByUserId(command.userId())).willReturn(Optional.of(employer));
        given(jobPostingJpaRepository.save(any(JobPostingEntity.class))).willReturn(
                JobPostingEntity.builder()
                        .id(1L)
                        .employer(employer)
                        .title(command.title())
                        .description(command.description())
                        .location(geometryFactory.createPoint(new Coordinate(command.longitude(), command.latitude())))
                        .address(command.address())
                        .hourlyWage(command.hourlyWage())
                        .capacity(command.capacity())
                        .startAt(command.startAt())
                        .endAt(command.endAt())
                        .build()
        );

        // when
        Long jobPostingId = jobPostingService.create(command);

        // then
        assertThat(jobPostingId).isEqualTo(1L);
        verify(jobPostingJpaRepository).save(any(JobPostingEntity.class));
    }

    @Test
    @DisplayName("공고 등록 실패 - 구인자 프로필 없음")
    void create_fail_employer_not_found() {
        // given
        given(employerJpaRepository.findByUserId(command.userId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> jobPostingService.create(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.EMPLOYER_NOT_FOUND);
                });

        verify(jobPostingJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("공고 등록 실패 - 패널티 상태의 구인자")
    void create_fail_employer_penalized() {
        // given
        // 패널티 부여 (3회 노쇼 → 7일 제한)
        employer.increasePenalty();
        employer.increasePenalty();
        employer.increasePenalty();

        given(employerJpaRepository.findByUserId(command.userId())).willReturn(Optional.of(employer));

        // when & then
        assertThatThrownBy(() -> jobPostingService.create(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.EMPLOYER_PENALIZED);
                });

        verify(jobPostingJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("공고 등록 시 상태는 OPEN으로 저장된다")
    void create_status_is_open() {
        // given
        given(employerJpaRepository.findByUserId(command.userId())).willReturn(Optional.of(employer));
        given(jobPostingJpaRepository.save(any(JobPostingEntity.class))).willAnswer(invocation -> {
            JobPostingEntity saved = invocation.getArgument(0);
            assertThat(saved.getStatus().getDescription()).isEqualTo("모집중");
            return JobPostingEntity.builder()
                    .id(1L)
                    .employer(employer)
                    .title(saved.getTitle())
                    .location(saved.getLocation())
                    .address(saved.getAddress())
                    .hourlyWage(saved.getHourlyWage())
                    .capacity(saved.getCapacity())
                    .startAt(saved.getStartAt())
                    .endAt(saved.getEndAt())
                    .status(saved.getStatus())
                    .build();
        });

        // when
        Long jobPostingId = jobPostingService.create(command);

        // then
        assertThat(jobPostingId).isNotNull();
    }

    // ── getNearby 테스트 ──────────────────────────────────────────

    @Test
    @DisplayName("근처 공고 조회 성공 - 반경 내 공고 반환")
    void getNearby_success() {
        // given
        GetNearbyJobPostingsUseCase.Query query = new GetNearbyJobPostingsUseCase.Query(
                37.4979, 127.0276, 3000
        );

        LocalDateTime startAt = LocalDateTime.now().plusDays(1);
        LocalDateTime endAt = startAt.plusHours(4);

        List<JobPostingEntity> entities = List.of(
                JobPostingEntity.builder()
                        .id(1L)
                        .employer(employer)
                        .title("카페 알바")
                        .location(geometryFactory.createPoint(new Coordinate(127.0276, 37.4979)))
                        .address("서울시 강남구 역삼동")
                        .hourlyWage(10000)
                        .capacity(3)
                        .startAt(startAt)
                        .endAt(endAt)
                        .status(JobStatus.OPEN)
                        .build(),
                JobPostingEntity.builder()
                        .id(2L)
                        .employer(employer)
                        .title("편의점 알바")
                        .location(geometryFactory.createPoint(new Coordinate(127.0300, 37.5000)))
                        .address("서울시 강남구 삼성동")
                        .hourlyWage(9860)
                        .capacity(2)
                        .startAt(startAt)
                        .endAt(endAt)
                        .status(JobStatus.OPEN)
                        .build()
        );

        given(jobPostingJpaRepository.findNearby(query.latitude(), query.longitude(), query.radiusMeters()))
                .willReturn(entities);

        // when
        List<JobPosting> result = jobPostingService.getNearby(query);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("카페 알바");
        assertThat(result.get(1).getTitle()).isEqualTo("편의점 알바");
    }

    @Test
    @DisplayName("근처 공고 조회 - 반경 내 공고 없음")
    void getNearby_empty() {
        // given
        GetNearbyJobPostingsUseCase.Query query = new GetNearbyJobPostingsUseCase.Query(
                37.4979, 127.0276, 3000
        );

        given(jobPostingJpaRepository.findNearby(query.latitude(), query.longitude(), query.radiusMeters()))
                .willReturn(List.of());

        // when
        List<JobPosting> result = jobPostingService.getNearby(query);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("근처 공고 조회 - 도메인 객체로 올바르게 변환된다")
    void getNearby_mapped_correctly() {
        // given
        GetNearbyJobPostingsUseCase.Query query = new GetNearbyJobPostingsUseCase.Query(
                37.4979, 127.0276, 3000
        );

        LocalDateTime startAt = LocalDateTime.now().plusDays(1);
        LocalDateTime endAt = startAt.plusHours(4);

        JobPostingEntity entity = JobPostingEntity.builder()
                .id(1L)
                .employer(employer)
                .title("카페 알바")
                .location(geometryFactory.createPoint(new Coordinate(127.0276, 37.4979)))
                .address("서울시 강남구 역삼동")
                .hourlyWage(10000)
                .capacity(3)
                .startAt(startAt)
                .endAt(endAt)
                .status(JobStatus.OPEN)
                .build();

        given(jobPostingJpaRepository.findNearby(query.latitude(), query.longitude(), query.radiusMeters()))
                .willReturn(List.of(entity));

        // when
        List<JobPosting> result = jobPostingService.getNearby(query);

        // then
        JobPosting jobPosting = result.get(0);
        assertThat(jobPosting.getId()).isEqualTo(1L);
        assertThat(jobPosting.getTitle()).isEqualTo("카페 알바");
        assertThat(jobPosting.getLatitude()).isEqualTo(37.4979);   // Y = 위도
        assertThat(jobPosting.getLongitude()).isEqualTo(127.0276); // X = 경도
        assertThat(jobPosting.getHourlyWage()).isEqualTo(10000);
        assertThat(jobPosting.getStatus()).isEqualTo(JobStatus.OPEN);
    }
}
