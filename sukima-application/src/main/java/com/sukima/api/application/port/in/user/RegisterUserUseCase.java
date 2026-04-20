package com.sukima.api.application.port.in.user;

import com.sukima.api.domain.common.type.RoleType;

public interface RegisterUserUseCase {

    Long register(Command command);

    record Command(String email, String password, RoleType role) {}
}
