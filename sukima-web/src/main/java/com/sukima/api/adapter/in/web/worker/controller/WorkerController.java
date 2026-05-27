package com.sukima.api.adapter.in.web.worker.controller;

import com.sukima.api.adapter.in.web.worker.request.CheckRequest;
import com.sukima.api.adapter.in.web.worker.request.RegisterWorkerRequest;
import com.sukima.api.adapter.in.web.worker.response.ApplyJobResponse;
import com.sukima.api.adapter.in.web.worker.response.QrTokenResponse;
import com.sukima.api.adapter.in.web.worker.response.RegisterWorkerResponse;
import com.sukima.api.application.port.in.application.ApplyJobUseCase;
import com.sukima.api.application.port.in.qr.IssueQrTokenUseCase;
import com.sukima.api.application.port.in.worker.RegisterWorkerUseCase;
import com.sukima.api.application.port.in.worklog.CheckInUseCase;
import com.sukima.api.application.port.in.worklog.CheckOutUseCase;
import com.sukima.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Worker", description = "구직자 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final RegisterWorkerUseCase registerWorkerUseCase;
    private final ApplyJobUseCase applyJobUseCase;
    private final CheckInUseCase checkInUseCase;
    private final CheckOutUseCase checkOutUseCase;
    private final IssueQrTokenUseCase issueQrTokenUseCase;

    @Operation(summary = "구직자 프로필 등록")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<RegisterWorkerResponse>> registerProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegisterWorkerRequest request) {

        Long workerId = registerWorkerUseCase.register(
                new RegisterWorkerUseCase.Command(userId, request.name(), request.phone())
        );
        return ResponseEntity.ok(ApiResponse.ok(new RegisterWorkerResponse(workerId)));
    }

    @Operation(summary = "공고 지원")
    @PostMapping("/jobs/{jobPostingId}/apply")
    public ResponseEntity<ApiResponse<ApplyJobResponse>> applyJob(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "공고 ID") @PathVariable Long jobPostingId) {

        Long applicationId = applyJobUseCase.apply(
                new ApplyJobUseCase.Command(jobPostingId, userId)
        );
        return ResponseEntity.ok(ApiResponse.ok(new ApplyJobResponse(applicationId)));
    }

    @Operation(summary = "QR 토큰 발급", description = "만료: 근무 종료 + 30분")
    @PostMapping("/matches/{matchId}/qr-token")
    public ResponseEntity<ApiResponse<QrTokenResponse>> issueQrToken(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "매칭 ID") @PathVariable Long matchId) {

        IssueQrTokenUseCase.Result result = issueQrTokenUseCase.issue(
                new IssueQrTokenUseCase.Command(matchId, userId, false)
        );
        return ResponseEntity.ok(ApiResponse.ok(new QrTokenResponse(result.qrToken(), result.expiresAt())));
    }

    @Operation(summary = "QR 토큰 재발급", description = "이전 토큰 무효화")
    @PostMapping("/matches/{matchId}/qr-token/reissue")
    public ResponseEntity<ApiResponse<QrTokenResponse>> reissueQrToken(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "매칭 ID") @PathVariable Long matchId) {

        IssueQrTokenUseCase.Result result = issueQrTokenUseCase.issue(
                new IssueQrTokenUseCase.Command(matchId, userId, true)
        );
        return ResponseEntity.ok(ApiResponse.ok(new QrTokenResponse(result.qrToken(), result.expiresAt())));
    }

    @Operation(summary = "QR 체크인", description = "근무지 반경 100m 이내에서만 가능")
    @PostMapping("/matches/{matchId}/checkin")
    public ResponseEntity<ApiResponse<Void>> checkIn(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "매칭 ID") @PathVariable Long matchId,
            @Valid @RequestBody CheckRequest request) {

        checkInUseCase.checkIn(
                new CheckInUseCase.Command(matchId, userId, request.qrToken(), request.latitude(), request.longitude())
        );
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "QR 체크아웃", description = "정산 자동 생성, 매칭 COMPLETED 처리")
    @PostMapping("/matches/{matchId}/checkout")
    public ResponseEntity<ApiResponse<Void>> checkOut(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "매칭 ID") @PathVariable Long matchId,
            @Valid @RequestBody CheckRequest request) {

        checkOutUseCase.checkOut(
                new CheckOutUseCase.Command(matchId, userId, request.qrToken(), request.latitude(), request.longitude())
        );
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
