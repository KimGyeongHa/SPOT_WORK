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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final ApplyJobUseCase applyJobUseCase;
    private final CheckInUseCase checkInUseCase;
    private final CheckOutUseCase checkOutUseCase;
    private final IssueQrTokenUseCase issueQrTokenUseCase;

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
            @Valid @RequestBody RegisterWorkerRequest request) {

        Long workerId = registerWorkerUseCase.register(
                new RegisterWorkerUseCase.Command(userId, request.name(), request.phone())
        );
        return ResponseEntity.ok(new RegisterWorkerResponse(workerId));
    }

    @Operation(summary = "공고 지원", description = "구직자가 공고에 지원합니다. 중복 지원은 불가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지원 성공",
                    content = @Content(schema = @Schema(implementation = ApplyJobResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미 지원한 공고", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공고 또는 구직자", content = @Content)
    })
    @PostMapping("/jobs/{jobPostingId}/apply")
    public ResponseEntity<ApplyJobResponse> applyJob(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "지원할 공고 ID", example = "1") @PathVariable Long jobPostingId) {

        Long applicationId = applyJobUseCase.apply(
                new ApplyJobUseCase.Command(jobPostingId, userId)
        );
        return ResponseEntity.ok(new ApplyJobResponse(applicationId));
    }

    @Operation(summary = "QR 토큰 발급",
            description = "체크인/체크아웃에 사용할 QR 토큰을 발급합니다. 만료: 근무 종료 시각 + 30분.")
    @PostMapping("/matches/{matchId}/qr-token")
    public ResponseEntity<QrTokenResponse> issueQrToken(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "매칭 ID", example = "1") @PathVariable Long matchId) {

        IssueQrTokenUseCase.Result result = issueQrTokenUseCase.issue(
                new IssueQrTokenUseCase.Command(matchId, userId, false)
        );
        return ResponseEntity.ok(new QrTokenResponse(result.qrToken(), result.expiresAt()));
    }

    @Operation(summary = "QR 토큰 재발급",
            description = "QR 분실 시 재발급합니다. 이전 토큰은 무효화됩니다.")
    @PostMapping("/matches/{matchId}/qr-token/reissue")
    public ResponseEntity<QrTokenResponse> reissueQrToken(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "매칭 ID", example = "1") @PathVariable Long matchId) {

        IssueQrTokenUseCase.Result result = issueQrTokenUseCase.issue(
                new IssueQrTokenUseCase.Command(matchId, userId, true)
        );
        return ResponseEntity.ok(new QrTokenResponse(result.qrToken(), result.expiresAt()));
    }

    @Operation(summary = "QR 체크인",
            description = "근무 시작 시 QR 스캔으로 체크인합니다. 근무지 반경 100m 이내에서만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "체크인 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 QR / 매칭 상태 부적합 / 근무지 반경 벗어남", content = @Content),
            @ApiResponse(responseCode = "403", description = "본인의 매칭이 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매칭", content = @Content)
    })
    @PostMapping("/matches/{matchId}/checkin")
    public ResponseEntity<Void> checkIn(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "매칭 ID", example = "1") @PathVariable Long matchId,
            @Valid @RequestBody CheckRequest request) {

        checkInUseCase.checkIn(
                new CheckInUseCase.Command(matchId, userId, request.qrToken(), request.latitude(), request.longitude())
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "QR 체크아웃",
            description = "근무 종료 시 QR 스캔으로 체크아웃합니다. 체크아웃 시 정산이 자동 생성되고 매칭 상태가 COMPLETED로 변경됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "체크아웃 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 QR / 체크인 안 됨 / 이미 체크아웃됨 / 근무지 반경 벗어남", content = @Content),
            @ApiResponse(responseCode = "403", description = "본인의 매칭이 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매칭", content = @Content)
    })
    @PostMapping("/matches/{matchId}/checkout")
    public ResponseEntity<Void> checkOut(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "매칭 ID", example = "1") @PathVariable Long matchId,
            @Valid @RequestBody CheckRequest request) {

        checkOutUseCase.checkOut(
                new CheckOutUseCase.Command(matchId, userId, request.qrToken(), request.latitude(), request.longitude())
        );
        return ResponseEntity.ok().build();
    }
}
