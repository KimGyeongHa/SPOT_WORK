package com.sukima.api.domain.match.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchStatus {
    CONFIRMED("매칭확정"),
    COMPLETED("근무완료"),
    CANCELLED("취소됨");

    private final String description;
}
