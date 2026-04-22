package com.sukima.api.adapter.in.web.auth.controller;

import com.sukima.api.adapter.in.web.auth.request.LoginRequest;
import com.sukima.api.adapter.in.web.auth.request.RefreshRequest;
import com.sukima.api.adapter.in.web.auth.request.RegisterRequest;
import com.sukima.api.adapter.in.web.auth.response.LoginResponse;
import com.sukima.api.adapter.in.web.auth.response.RegisterResponse;
import com.sukima.api.application.port.in.user.LoginUseCase;
import com.sukima.api.application.port.in.user.RegisterUserUseCase;
import com.sukima.api.security.jwt.JwtTokenProvider;
import com.sukima.api.security.jwt.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API (회원가입, 로그인, 토큰 재발급, 로그아웃)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 역할(WORKER/EMPLOYER)로 회원가입합니다.")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        Long userId = registerUserUseCase.register(
                new RegisterUserUseCase.Command(request.email(), request.password(), request.role())
        );
        return ResponseEntity.ok(new RegisterResponse(userId));
    }

    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인 후 AccessToken(15분)과 RefreshToken(7일)을 발급합니다.")
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

    @Operation(summary = "토큰 재발급", description = "RefreshToken으로 AccessToken과 RefreshToken을 재발급합니다. (RTR 방식 - 기존 RefreshToken 폐기)")
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

    @Operation(summary = "로그아웃", description = "RefreshToken을 Redis에서 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        Long userId = jwtTokenProvider.getUserId(request.refreshToken());
        refreshTokenService.delete(userId);
        return ResponseEntity.ok().build();
    }
}
