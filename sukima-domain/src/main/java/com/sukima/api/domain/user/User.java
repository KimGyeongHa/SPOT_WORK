package com.sukima.api.domain.user;

import com.sukima.api.domain.common.type.RoleType;
import lombok.Builder;
import lombok.Getter;

@Getter
public class User {

    private final Long id;
    private final String email;
    private final String password;
    private final RoleType role;

    @Builder
    public User(Long id, String email, String password, RoleType role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
