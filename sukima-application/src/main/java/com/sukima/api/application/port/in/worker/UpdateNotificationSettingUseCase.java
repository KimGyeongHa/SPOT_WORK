package com.sukima.api.application.port.in.worker;

public interface UpdateNotificationSettingUseCase {

    void update(Command command);

    record Command(
            Long userId,
            boolean enabled,
            Double lat,
            Double lng,
            Integer radiusMeters
    ) {}
}
