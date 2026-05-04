package com.sukima.api.application.port.in.qr;

import java.time.LocalDateTime;

public interface IssueQrTokenUseCase {

    Result issue(Command command);

    record Command(Long matchId, Long userId, boolean reissue) {}

    record Result(String qrToken, LocalDateTime expiresAt) {}
}
