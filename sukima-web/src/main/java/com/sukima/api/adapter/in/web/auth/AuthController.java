package com.sukima.api.adapter.in.web.auth;

import com.sukima.api.application.port.in.user.LoginUseCase;
import com.sukima.api.application.port.in.user.RegisterUserUseCase;
import com.sukima.api.security.jwt.JwtTokenProvider;
import com.sukima.api.security.jwt.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API (회원가입, 로그인, 토큰 갱신, 로그아웃)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 역할(WORKER/EMPLOYER)로 신규 회원을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content)
    })
    @SecurityRequirements  // 회원가입은 인증 불필요
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        Long userId = registerUserUseCase.register(
                new RegisterUserUseCase.Command(request.email(), request.password(), request.role())
        );
        return ResponseEntity.ok(new RegisterResponse(userId));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 Access Token과 Refresh Token을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 불일치)", content = @Content)
    })
    @SecurityRequirements  // 로그인은 인증 불필요
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginUseCase.Result result = loginUseCase.login(
                new LoginUseCase.Command(request.email(), request.password())
        );

        String accessToken = jwtTokenProvider.generateAccessToken(result.userId(), result.role());
        String refreshToken = jwtTokenProvider.generateRefreshToken(result.userId());

        refreshTokenService.save(result.userId(), refreshToken);

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken));
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token을 이용해 새로운 Access Token과 Refresh Token을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 Refresh Token", content = @Content)
    })
    @SecurityRequirements  // Refresh는 토큰을 body로 받으므로 인증 헤더 불필요
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        if (!refreshTokenService.validate(userId, refreshToken)) {
            throw new IllegalArgumentException("리프레시 토큰이 일치하지 않습니다.");
        }

        refreshTokenService.delete(userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId,
                jwtTokenProvider.getRole(refreshToken) != null
                        ? jwtTokenProvider.getRole(refreshToken)
                        : "WORKER");
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        refreshTokenService.save(userId, newRefreshToken);

        return ResponseEntity.ok(new LoginResponse(newAccessToken, newRefreshToken));
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화하여 로그아웃 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        Long userId = jwtTokenProvider.getUserId(request.refreshToken());
        refreshTokenService.delete(userId);
        return ResponseEntity.ok().build();
    }
}
