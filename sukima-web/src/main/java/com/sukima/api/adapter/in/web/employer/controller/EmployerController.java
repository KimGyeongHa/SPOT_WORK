package com.sukima.api.adapter.in.web.employer.controller;

import com.sukima.api.adapter.in.web.employer.request.EmployerProfileRequest;
import com.sukima.api.adapter.in.web.employer.request.JobPostingRequest;
import com.sukima.api.adapter.in.web.employer.response.AcceptResponse;
import com.sukima.api.adapter.in.web.employer.response.EmployerProfileResponse;
import com.sukima.api.adapter.in.web.employer.response.JobPostingResponse;
import com.sukima.api.application.port.in.application.AcceptApplicationUseCase;
import com.sukima.api.application.port.in.employer.RegisterEmployerUseCase;
import com.sukima.api.application.port.in.job.CreateJobPostingUseCase;
import com.sukima.api.application.port.in.payment.CompletePaymentUseCase;
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

@Tag(name = "Employer", description = "구인자 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final RegisterEmployerUseCase registerEmployerUseCase;
    private final CreateJobPostingUseCase createJobPostingUseCase;
    private final AcceptApplicationUseCase acceptApplicationUseCase;
    private final CompletePaymentUseCase completePaymentUseCase;

    @Operation(summary = "구인자 프로필 등록")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<EmployerProfileResponse>> registerProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody EmployerProfileRequest request) {

        Long employerId = registerEmployerUseCase.register(
                new RegisterEmployerUseCase.Command(userId, request.name(), request.phone(), request.companyName())
        );
        return ResponseEntity.ok(ApiResponse.ok(new EmployerProfileResponse(employerId)));
    }

    @Operation(summary = "공고 등록")
    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<JobPostingResponse>> createJobPosting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody JobPostingRequest request) {

        Long jobPostingId = createJobPostingUseCase.create(
                new CreateJobPostingUseCase.Command(
                        userId, request.title(), request.description(),
                        request.latitude(), request.longitude(), request.address(),
                        request.hourlyWage(), request.capacity(), request.startAt(), request.endAt()
                )
        );
        return ResponseEntity.ok(ApiResponse.ok(new JobPostingResponse(jobPostingId)));
    }

    @Operation(summary = "지원 수락", description = "Redisson 분산락으로 정원 초과 방지")
    @PostMapping("/applications/{applicationId}/accept")
    public ResponseEntity<ApiResponse<AcceptResponse>> acceptApplication(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "지원 ID") @PathVariable Long applicationId) {

        Long matchId = acceptApplicationUseCase.accept(
                new AcceptApplicationUseCase.Command(applicationId, userId)
        );
        return ResponseEntity.ok(ApiResponse.ok(new AcceptResponse(matchId)));
    }

    @Operation(summary = "정산 완료 처리", description = "외부 결제 API 성공 콜백")
    @PostMapping("/payments/{paymentId}/complete")
    public ResponseEntity<ApiResponse<Void>> completePayment(
            @Parameter(description = "정산 ID") @PathVariable Long paymentId) {
        completePaymentUseCase.complete(paymentId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "정산 실패 처리", description = "외부 결제 API 실패 콜백")
    @PostMapping("/payments/{paymentId}/fail")
    public ResponseEntity<ApiResponse<Void>> failPayment(
            @Parameter(description = "정산 ID") @PathVariable Long paymentId) {
        completePaymentUseCase.fail(paymentId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
