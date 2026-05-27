package com.sukima.api.adapter.in.web.auth.controller;

import com.sukima.api.adapter.in.web.auth.request.LoginRequest;
import com.sukima.api.adapter.in.web.auth.request.RefreshRequest;
import com.sukima.api.adapter.in.web.auth.request.RegisterRequest;
import com.sukima.api.adapter.in.web.auth.response.LoginResponse;
import com.sukima.api.adapter.in.web.auth.response.RegisterResponse;
import com.sukima.api.application.port.in.user.LoginUseCase;
import com.sukima.api.application.port.in.user.RegisterUserUseCase;
import com.sukima.api.common.response.ApiResponse;
import com.sukima.api.domain.common.exception.BusinessException;
import com.sukima.api.domain.common.exception.ErrorCode;
import com.sukima.api.security.jwt.AccessTokenService;
import com.sukima.api.security.jwt.JwtTokenProvider;
import com.sukima.api.security.jwt.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;

    @Operation(summary = "회원가입")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = registerUserUseCase.register(
                new RegisterUserUseCase.Command(request.email(), request.password(), request.role())
        );
        return ResponseEntity.ok(ApiResponse.ok(new RegisterResponse(userId)));
    }

    @Operation(summary = "로그인", description = "새 기기 로그인 시 기존 기기 AccessToken 무효화")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginUseCase.Result result = loginUseCase.login(
                new LoginUseCase.Command(request.email(), request.password())
        );

        String accessToken = jwtTokenProvider.generateAccessToken(result.userId(), result.role());
        String refreshToken = jwtTokenProvider.generateRefreshToken(result.userId());

        accessTokenService.save(result.userId(), accessToken);
        refreshTokenService.save(result.userId(), refreshToken);

        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(accessToken, refreshToken)));
    }

    @Operation(summary = "토큰 재발급", description = "RTR 방식 - 기존 토큰 폐기 후 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        if (!refreshTokenService.validate(userId, refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String role = jwtTokenProvider.getRole(refreshToken) != null
                ? jwtTokenProvider.getRole(refreshToken) : "WORKER";

        accessTokenService.delete(userId);
        refreshTokenService.delete(userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, role);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        accessTokenService.save(userId, newAccessToken);
        refreshTokenService.save(userId, newRefreshToken);

        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(newAccessToken, newRefreshToken)));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        Long userId = jwtTokenProvider.getUserId(request.refreshToken());
        accessTokenService.delete(userId);
        refreshTokenService.delete(userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
