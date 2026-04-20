package com.sukima.api.domain.job.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobStatus {
    OPEN("모집중"),
    CLOSED("모집완료"),
    CANCELLED("취소됨");

    private final String description;
}
