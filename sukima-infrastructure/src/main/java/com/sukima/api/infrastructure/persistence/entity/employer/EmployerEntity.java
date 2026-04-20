package com.sukima.api.infrastructure.persistence.entity.employer;

import com.sukima.api.infrastructure.persistence.base.BaseTimeEntity;
import com.sukima.api.infrastructure.persistence.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "employers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployerEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserEntity user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Builder
    public EmployerEntity(Long id, UserEntity user, String name, String phone, String companyName, BigDecimal rating) {
        this.id = id;
        this.user = user;
        this.name = name;
        this.phone = phone;
        this.companyName = companyName;
        this.rating = rating;
    }
}
