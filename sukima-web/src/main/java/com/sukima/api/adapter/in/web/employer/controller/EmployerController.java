package com.sukima.api.adapter.in.web.employer.controller;

import com.sukima.api.adapter.in.web.employer.request.EmployerProfileRequest;
import com.sukima.api.adapter.in.web.employer.request.JobPostingRequest;
import com.sukima.api.adapter.in.web.employer.response.EmployerProfileResponse;
import com.sukima.api.adapter.in.web.employer.response.JobPostingResponse;
import com.sukima.api.application.port.in.employer.RegisterEmployerUseCase;
import com.sukima.api.application.port.in.job.CreateJobPostingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Employer", description = "구인자 API (프로필 등록, 공고 등록)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final RegisterEmployerUseCase registerEmployerUseCase;
    private final CreateJobPostingUseCase createJobPostingUseCase;

    @Operation(summary = "구인자 프로필 등록", description = "로그인 후 구인자 프로필(이름, 연락처, 회사명)을 등록합니다.")
    @PostMapping("/profile")
    public ResponseEntity<EmployerProfileResponse> registerProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody EmployerProfileRequest request) {

        Long employerId = registerEmployerUseCase.register(
                new RegisterEmployerUseCase.Command(userId, request.name(), request.phone(), request.companyName())
        );
        return ResponseEntity.ok(new EmployerProfileResponse(employerId));
    }

    @Operation(summary = "공고 등록", description = "구인자가 단기 알바 공고를 등록합니다. 위치 정보(위경도)를 포함하며 PostGIS로 저장됩니다.")
    @PostMapping("/jobs")
    public ResponseEntity<JobPostingResponse> createJobPosting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody JobPostingRequest request) {

        Long jobPostingId = createJobPostingUseCase.create(
                new CreateJobPostingUseCase.Command(
                        userId,
                        request.title(),
                        request.description(),
                        request.latitude(),
                        request.longitude(),
                        request.address(),
                        request.hourlyWage(),
                        request.capacity(),
                        request.startAt(),
                        request.endAt()
                )
        );
        return ResponseEntity.ok(new JobPostingResponse(jobPostingId));
    }
}
