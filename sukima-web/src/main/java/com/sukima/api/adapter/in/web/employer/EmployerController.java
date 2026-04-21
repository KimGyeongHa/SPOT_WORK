package com.sukima.api.adapter.in.web.employer;

import com.sukima.api.application.port.in.employer.RegisterEmployerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Employer", description = "고용주 관련 API")
@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final RegisterEmployerUseCase registerEmployerUseCase;

    @Operation(summary = "고용주 프로필 등록", description = "로그인 후 고용주(EMPLOYER) 프로필을 등록합니다. EMPLOYER 역할의 JWT가 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 등록 성공",
                    content = @Content(schema = @Schema(implementation = RegisterEmployerResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미 등록된 프로필", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (EMPLOYER 역할 필요)", content = @Content)
    })
    @PostMapping("/profile")
    public ResponseEntity<RegisterEmployerResponse> registerProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody RegisterEmployerRequest request
    ) {
        Long employerId = registerEmployerUseCase.register(
                new RegisterEmployerUseCase.Command(userId, request.name(), request.phone(), request.companyName())
        );
        return ResponseEntity.ok(new RegisterEmployerResponse(employerId));
    }
}
