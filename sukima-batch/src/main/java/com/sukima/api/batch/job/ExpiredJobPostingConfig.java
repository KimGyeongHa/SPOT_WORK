package com.sukima.api.batch.job;

import com.sukima.api.domain.job.type.JobStatus;
import com.sukima.api.infrastructure.persistence.entity.job.JobPostingEntity;
import com.sukima.api.infrastructure.persistence.repository.JobPostingJpaRepository;
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
public class ExpiredJobPostingConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final JobPostingJpaRepository jobPostingJpaRepository;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job expiredJobPostingJob() {
        return new JobBuilder("expiredJobPostingJob", jobRepository)
                .start(expiredJobPostingStep())
                .build();
    }

    @Bean
    public Step expiredJobPostingStep() {
        return new StepBuilder("expiredJobPostingStep", jobRepository)
                .<JobPostingEntity, JobPostingEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredJobPostingReader())
                .processor(expiredJobPostingProcessor())
                .writer(expiredJobPostingWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<JobPostingEntity> expiredJobPostingReader() {
        return new JpaPagingItemReaderBuilder<JobPostingEntity>()
                .name("expiredJobPostingReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("""
                        SELECT j FROM JobPostingEntity j
                        WHERE j.status = 'OPEN'
                        AND j.endAt < :now
                        """)
                .parameterValues(Map.of("now", LocalDateTime.now()))
                .build();
    }

    @Bean
    public ItemProcessor<JobPostingEntity, JobPostingEntity> expiredJobPostingProcessor() {
        return posting -> {
            log.info("만료 공고 처리: id={}, title={}", posting.getId(), posting.getTitle());
            posting.close();
            return posting;
        };
    }

    @Bean
    public ItemWriter<JobPostingEntity> expiredJobPostingWriter() {
        return chunk -> {
            jobPostingJpaRepository.saveAll(chunk.getItems());
            log.info("만료 공고 {}건 CLOSED 처리 완료", chunk.getItems().size());
        };
    }
}
