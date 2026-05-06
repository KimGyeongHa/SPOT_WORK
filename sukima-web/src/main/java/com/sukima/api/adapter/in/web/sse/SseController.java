package com.sukima.api.adapter.in.web.sse;

import com.sukima.api.sse.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE", description = "실시간 알림 (Server-Sent Events)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    @Operation(summary = "SSE 연결",
            description = "실시간 알림을 수신하기 위한 SSE 연결을 생성합니다. " +
                    "매칭 확정, 노쇼 감지 등의 이벤트를 실시간으로 수신할 수 있습니다. " +
                    "새 기기에서 연결 시 기존 연결은 자동 종료됩니다.")
    @GetMapping(value = "/connect", produces = "text/event-stream")
    public SseEmitter connect(@AuthenticationPrincipal Long userId) {
        return sseEmitterService.connect(userId);
    }
}
