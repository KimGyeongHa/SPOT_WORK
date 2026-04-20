package com.sukima.api.application.port.in.worker;

public interface RegisterWorkerUseCase {

    Long register(Command command);

    record Command(Long userId, String name, String phone) {}
}
