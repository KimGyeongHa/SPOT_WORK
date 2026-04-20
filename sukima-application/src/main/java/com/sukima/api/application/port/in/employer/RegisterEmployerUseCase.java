package com.sukima.api.application.port.in.employer;

public interface RegisterEmployerUseCase {

    Long register(Command command);

    record Command(Long userId, String name, String phone, String companyName) {}
}
