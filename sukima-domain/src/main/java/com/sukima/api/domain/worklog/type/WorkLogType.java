package com.sukima.api.domain.worklog.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkLogType {
    CHECK_IN("체크인"),
    CHECK_OUT("체크아웃");

    private final String description;
}
