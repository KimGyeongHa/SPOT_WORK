package com.sukima.api.application.port.in.user;

public interface LoginUseCase {

    Result login(Command command);

    record Command(String email, String password) {}
    record Result(Long userId, String role, String email) {}
}
