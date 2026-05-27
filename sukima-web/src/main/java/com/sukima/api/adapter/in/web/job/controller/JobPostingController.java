package com.sukima.api.adapter.in.web.job.controller;

import com.sukima.api.adapter.in.web.job.response.NearbyJobPostingResponse;
import com.sukima.api.application.port.in.job.GetNearbyJobPostingsUseCase;
import com.sukima.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "JobPosting", description = "공고 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobPostingController {

    private final GetNearbyJobPostingsUseCase getNearbyJobPostingsUseCase;

    @Operation(summary = "근처 공고 조회", description = "현재 위치 기준 반경 N미터 이내 OPEN 상태 공고 조회")
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyJobPostingResponse>>> getNearby(
            @Parameter(description = "위도", example = "37.4979") @RequestParam double latitude,
            @Parameter(description = "경도", example = "127.0276") @RequestParam double longitude,
            @Parameter(description = "반경 (미터)", example = "3000") @RequestParam(defaultValue = "3000") double radius) {

        List<NearbyJobPostingResponse> result = getNearbyJobPostingsUseCase
                .getNearby(new GetNearbyJobPostingsUseCase.Query(latitude, longitude, radius))
                .stream()
                .map(NearbyJobPostingResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
