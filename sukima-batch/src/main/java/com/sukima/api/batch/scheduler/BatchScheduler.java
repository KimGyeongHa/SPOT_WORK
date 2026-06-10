package com.sukima.api.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job expiredJobPostingJob;
    private final Job penaltyExpiredJob;

    @Scheduled(cron = "0 0 * * * *")
    public void runExpiredJobPostingJob() {
        run(expiredJobPostingJob, "expiredJobPostingJob");
    }

    @Scheduled(cron = "0 30 * * * *")
    public void runPenaltyExpiredJob() {
        run(penaltyExpiredJob, "penaltyExpiredJob");
    }

    private void run(Job job, String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(job, params);
            log.info("배치 실행 완료: {}", jobName);
        } catch (Exception e) {
            log.error("배치 실행 실패: {}", jobName, e);
        }
    }
}
