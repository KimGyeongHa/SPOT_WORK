package com.sukima.api.adapter.in.web.auth;

import com.sukima.api.application.port.in.user.LoginUseCase;
import com.sukima.api.application.port.in.user.RegisterUserUseCase;
import com.sukima.api.security.jwt.JwtTokenProvider;
import com.sukima.api.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        Long userId = registerUserUseCase.register(
                new RegisterUserUseCase.Command(request.email(), request.password(), request.role())
        );
        return ResponseEntity.ok(new RegisterResponse(userId));
    }

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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        Long userId = jwtTokenProvider.getUserId(request.refreshToken());
        refreshTokenService.delete(userId);
        return ResponseEntity.ok().build();
    }
}
