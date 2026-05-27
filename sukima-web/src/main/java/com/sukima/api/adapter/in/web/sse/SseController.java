package com.sukima.api.adapter.in.web.sse;

import com.sukima.api.application.port.in.notification.GetNotificationsUseCase;
import com.sukima.api.common.response.ApiResponse;
import com.sukima.api.sse.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "SSE", description = "실시간 알림")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;
    private final GetNotificationsUseCase getNotificationsUseCase;

    @Operation(summary = "SSE 연결", description = "실시간 알림 수신을 위한 SSE 연결을 생성합니다.")
    @GetMapping(value = "/connect", produces = "text/event-stream")
    public SseEmitter connect(@AuthenticationPrincipal Long userId) {
        return sseEmitterService.connect(userId);
    }

    @Operation(summary = "미수신 알림 조회", description = "SSE 미연결 중 놓친 미읽은 알림 목록을 조회합니다.")
    @GetMapping("/notifications/unread")
    public ResponseEntity<ApiResponse<List<GetNotificationsUseCase.NotificationInfo>>> getUnread(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(getNotificationsUseCase.getUnread(userId)));
    }

    @Operation(summary = "알림 읽음 처리")
    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "알림 ID") @PathVariable Long notificationId) {
        getNotificationsUseCase.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
