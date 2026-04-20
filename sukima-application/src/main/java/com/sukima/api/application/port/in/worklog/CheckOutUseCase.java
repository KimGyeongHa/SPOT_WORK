package com.sukima.api.application.port.in.worklog;

public interface CheckOutUseCase {

    void checkOut(Command command);

    record Command(Long matchId, Long workerId, double latitude, double longitude) {}
}
