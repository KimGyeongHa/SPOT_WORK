package com.sukima.api.domain.common.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    WORKER("구직자"),
    EMPLOYER("구인자");

    private final String description;
}
