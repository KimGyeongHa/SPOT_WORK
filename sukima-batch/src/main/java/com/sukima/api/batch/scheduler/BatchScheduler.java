package com.sukima.api.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("expiredJobPostingJob")
    private final Job expiredJobPostingJob;

    @Qualifier("penaltyExpiredJob")
    private final Job penaltyExpiredJob;

    // 매 시간마다 만료 공고 처리
    @Scheduled(cron = "0 0 * * * *")
    public void runExpiredJobPostingJob() {
        run(expiredJobPostingJob, "expiredJobPostingJob");
    }

    // 매 시간마다 패널티 만료 알림
    @Scheduled(cron = "0 30 * * * *")
    public void runPenaltyExpiredJob() {
        run(penaltyExpiredJob, "penaltyExpiredJob");
    }

    private void run(Job job, String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis()) // 매 실행마다 고유 파라미터
                    .toJobParameters();

            jobLauncher.run(job, params);
            log.info("배치 실행 완료: {}", jobName);
        } catch (Exception e) {
            log.error("배치 실행 실패: {}", jobName, e);
        }
    }
}
