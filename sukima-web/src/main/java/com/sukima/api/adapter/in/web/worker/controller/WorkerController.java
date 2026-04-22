package com.sukima.api.adapter.in.web.worker.controller;

import com.sukima.api.adapter.in.web.worker.request.RegisterWorkerRequest;
import com.sukima.api.adapter.in.web.worker.response.RegisterWorkerResponse;
import com.sukima.api.application.port.in.worker.RegisterWorkerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Worker", description = "구직자 관련 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final RegisterWorkerUseCase registerWorkerUseCase;

    @Operation(summary = "구직자 프로필 등록", description = "로그인 후 구직자(WORKER) 프로필을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 등록 성공",
                    content = @Content(schema = @Schema(implementation = RegisterWorkerResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미 등록된 프로필", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (WORKER 역할 필요)", content = @Content)
    })
    @PostMapping("/profile")
    public ResponseEntity<RegisterWorkerResponse> registerProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody RegisterWorkerRequest request) {

        Long workerId = registerWorkerUseCase.register(
                new RegisterWorkerUseCase.Command(userId, request.name(), request.phone())
        );
        return ResponseEntity.ok(new RegisterWorkerResponse(workerId));
    }
}
