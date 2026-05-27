package com.sukima.api.application.service;

import com.sukima.api.application.port.in.notification.GetNotificationsUseCase;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.infrastructure.persistence.entity.notification.NotificationEntity;
import com.sukima.api.infrastructure.persistence.repository.NotificationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationJpaRepository notificationJpaRepository;

    private NotificationEntity notification;

    @BeforeEach
    void setUp() {
        notification = NotificationEntity.builder()
                .userId(1L)
                .type("MATCH_CONFIRMED")
                .message("카페 알바 공고에 매칭되었습니다!")
                .referenceId(1L)
                .build();
    }

    @Test
    @DisplayName("미읽은 알림 조회 성공")
    void getUnread_success() {
        // given
        given(notificationJpaRepository.findAllByUserIdAndReadYnOrderByCreatedAtDesc(1L, "N"))
                .willReturn(List.of(notification));

        // when
        List<GetNotificationsUseCase.NotificationInfo> result = notificationService.getUnread(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("MATCH_CONFIRMED");
        assertThat(result.get(0).message()).isEqualTo("카페 알바 공고에 매칭되었습니다!");
        assertThat(result.get(0).referenceId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("미읽은 알림 없음")
    void getUnread_empty() {
        // given
        given(notificationJpaRepository.findAllByUserIdAndReadYnOrderByCreatedAtDesc(1L, "N"))
                .willReturn(List.of());

        // when
        List<GetNotificationsUseCase.NotificationInfo> result = notificationService.getUnread(1L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_success() {
        // given
        given(notificationJpaRepository.findById(1L)).willReturn(Optional.of(notification));

        // when
        notificationService.markAsRead(1L, 1L);

        // then
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 존재하지 않는 알림")
    void markAsRead_fail_not_found() {
        // given
        given(notificationJpaRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 본인 알림이 아님")
    void markAsRead_fail_forbidden() {
        // given
        given(notificationJpaRepository.findById(1L)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }
}
