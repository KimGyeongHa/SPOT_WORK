package com.sukima.api.batch.job;

import com.sukima.api.application.port.out.notification.NotificationPort;
import com.sukima.api.infrastructure.persistence.entity.employer.EmployerEntity;
import com.sukima.api.infrastructure.persistence.entity.worker.WorkerEntity;
import com.sukima.api.infrastructure.persistence.repository.EmployerJpaRepository;
import com.sukima.api.infrastructure.persistence.repository.WorkerJpaRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PenaltyExpiredJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final WorkerJpaRepository workerJpaRepository;
    private final EmployerJpaRepository employerJpaRepository;
    private final NotificationPort notificationPort;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job penaltyExpiredJob() {
        return new JobBuilder("penaltyExpiredJob", jobRepository)
                .start(workerPenaltyExpiredStep())
                .next(employerPenaltyExpiredStep())
                .build();
    }

    // ── Worker 패널티 만료 Step ──────────────────────────────

    @Bean
    public Step workerPenaltyExpiredStep() {
        return new StepBuilder("workerPenaltyExpiredStep", jobRepository)
                .<WorkerEntity, WorkerEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredWorkerPenaltyReader())
                .processor(expiredWorkerPenaltyProcessor())
                .writer(expiredWorkerPenaltyWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<WorkerEntity> expiredWorkerPenaltyReader() {
        return new JpaPagingItemReaderBuilder<WorkerEntity>()
                .name("expiredWorkerPenaltyReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("""
                        SELECT w FROM WorkerEntity w
                        WHERE w.penaltyUntil IS NOT NULL
                        AND w.penaltyUntil < :now
                        """)
                .parameterValues(Map.of("now", LocalDateTime.now()))
                .build();
    }

    @Bean
    public ItemProcessor<WorkerEntity, WorkerEntity> expiredWorkerPenaltyProcessor() {
        return worker -> {
            log.info("Worker 패널티 만료: workerId={}", worker.getId());
            return worker;
        };
    }

    @Bean
    public ItemWriter<WorkerEntity> expiredWorkerPenaltyWriter() {
        return chunk -> {
            for (WorkerEntity worker : chunk.getItems()) {
                notificationPort.notifyPenaltyExpired(
                        worker.getUser().getId(),
                        "패널티가 해제되었습니다. 다시 공고에 지원할 수 있습니다."
                );
            }
            log.info("Worker 패널티 만료 알림 {}건 발송", chunk.getItems().size());
        };
    }

    // ── Employer 패널티 만료 Step ────────────────────────────

    @Bean
    public Step employerPenaltyExpiredStep() {
        return new StepBuilder("employerPenaltyExpiredStep", jobRepository)
                .<EmployerEntity, EmployerEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredEmployerPenaltyReader())
                .processor(expiredEmployerPenaltyProcessor())
                .writer(expiredEmployerPenaltyWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<EmployerEntity> expiredEmployerPenaltyReader() {
        return new JpaPagingItemReaderBuilder<EmployerEntity>()
                .name("expiredEmployerPenaltyReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("""
                        SELECT e FROM EmployerEntity e
                        WHERE e.penaltyUntil IS NOT NULL
                        AND e.penaltyUntil < :now
                        """)
                .parameterValues(Map.of("now", LocalDateTime.now()))
                .build();
    }

    @Bean
    public ItemProcessor<EmployerEntity, EmployerEntity> expiredEmployerPenaltyProcessor() {
        return employer -> {
            log.info("Employer 패널티 만료: employerId={}", employer.getId());
            return employer;
        };
    }

    @Bean
    public ItemWriter<EmployerEntity> expiredEmployerPenaltyWriter() {
        return chunk -> {
            for (EmployerEntity employer : chunk.getItems()) {
                notificationPort.notifyPenaltyExpired(
                        employer.getUser().getId(),
                        "패널티가 해제되었습니다. 다시 공고를 등록할 수 있습니다."
                );
            }
            log.info("Employer 패널티 만료 알림 {}건 발송", chunk.getItems().size());
        };
    }
}
