package com.sukima.api.infrastructure.persistence.entity.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "reference_id")
    private Long referenceId;

    // N: 미읽음, Y: 읽음
    @Column(name = "read_yn", nullable = false, length = 1)
    private String readYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    public NotificationEntity(Long userId, String type, String message, Long referenceId) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.referenceId = referenceId;
        this.readYn = "N";
        this.createdAt = LocalDateTime.now();
    }

    public void markRead() {
        this.readYn = "Y";
        this.readAt = LocalDateTime.now();
    }

    public boolean isRead() {
        return "Y".equals(this.readYn);
    }
}
