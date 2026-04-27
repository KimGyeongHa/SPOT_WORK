package com.sukima.api.infrastructure.persistence.entity.noshow;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "no_show_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoShowScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, unique = true)
    private Long matchId;

    // 노쇼 판정 트리거 시각 (근무 시작 + 15분)
    @Column(name = "trigger_at", nullable = false)
    private LocalDateTime triggerAt;

    // 처리 여부 (Y: 처리 완료, N: 미처리)
    @Column(name = "processed_yn", nullable = false, length = 1)
    private String processedYn;

    // 처리 완료 시각
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public NoShowScheduleEntity(Long matchId, LocalDateTime triggerAt, LocalDateTime createdAt) {
        this.matchId = matchId;
        this.triggerAt = triggerAt;
        this.processedYn = "N";
        this.createdAt = createdAt;
    }

    public void markProcessed() {
        this.processedYn = "Y";
        this.processedAt = LocalDateTime.now();
    }

    public boolean isProcessed() {
        return "Y".equals(this.processedYn);
    }
}
