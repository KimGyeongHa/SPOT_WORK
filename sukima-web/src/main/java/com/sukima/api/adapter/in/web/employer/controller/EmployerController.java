package com.sukima.api.adapter.in.web.employer.controller;

import com.sukima.api.adapter.in.web.employer.request.EmployerProfileRequest;
import com.sukima.api.adapter.in.web.employer.request.JobPostingRequest;
import com.sukima.api.adapter.in.web.employer.response.EmployerProfileResponse;
import com.sukima.api.adapter.in.web.employer.response.JobPostingResponse;
import com.sukima.api.application.port.in.employer.RegisterEmployerUseCase;
import com.sukima.api.application.port.in.job.CreateJobPostingUseCase;
import com.sukima.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
}
