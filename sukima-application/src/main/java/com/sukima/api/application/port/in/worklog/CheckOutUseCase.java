package com.sukima.api.application.port.in.worklog;

public interface CheckOutUseCase {

    void checkOut(Command command);

    record Command(Long matchId, Long userId, double latitude, double longitude) {}
}
