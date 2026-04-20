package com.sukima.api.adapter.in.web.auth;

import com.sukima.api.domain.common.type.RoleType;

public record RegisterRequest(String email, String password, RoleType role) {}
