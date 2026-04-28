package com.sukima.api.application.port.in.worklog;

public interface CheckInUseCase {

    void checkIn(Command command);

    record Command(Long matchId, Long userId, double latitude, double longitude) {}
}
