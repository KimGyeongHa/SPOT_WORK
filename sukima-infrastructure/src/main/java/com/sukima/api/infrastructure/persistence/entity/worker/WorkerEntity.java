package com.sukima.api.infrastructure.persistence.entity.worker;

import com.sukima.api.infrastructure.persistence.base.BaseTimeEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkerEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(name = "no_show_count", nullable = false)
    private int noShowCount = 0;

    @Column(name = "penalty_until")
    private LocalDateTime penaltyUntil;

    @Builder
    public WorkerEntity(Long id, UserEntity user, String name, String phone) {
        this.id = id;
        this.user = user;
        this.name = name;
        this.phone = phone;
        this.noShowCount = 0;
    }

    public void increaseNoShow() {
        this.noShowCount++;
        this.penaltyUntil = calculatePenaltyUntil(this.noShowCount);
    }

    public boolean isPenalized() {
        return penaltyUntil != null && penaltyUntil.isAfter(LocalDateTime.now());
    }

    private LocalDateTime calculatePenaltyUntil(int count) {
        if (count >= 5) return LocalDateTime.now().plusDays(30);
        if (count >= 3) return LocalDateTime.now().plusDays(7);
        return null;
    }
}
