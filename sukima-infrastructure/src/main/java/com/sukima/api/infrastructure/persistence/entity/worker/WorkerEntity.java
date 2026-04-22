package com.sukima.api.infrastructure.persistence.entity.worker;

import com.sukima.api.infrastructure.persistence.base.BaseTimeEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @Column(nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Builder
    public WorkerEntity(Long id, UserEntity user, String name, String phone, BigDecimal rating) {
        this.id = id;
        this.user = user;
        this.name = name;
        this.phone = phone;
        this.rating = rating;
    }
}
