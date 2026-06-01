package com.sukima.api.domain.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(400, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(500, "C002", "서버 내부 오류가 발생했습니다."),

    // 인증/인가
    UNAUTHORIZED(401, "A001", "인증이 필요합니다."),
    FORBIDDEN(403, "A002", "접근 권한이 없습니다."),
    INVALID_TOKEN(401, "A003", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, "A004", "만료된 토큰입니다."),

    // 유저
    DUPLICATE_EMAIL(400, "U001", "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(404, "U002", "존재하지 않는 사용자입니다."),
    INVALID_LOGIN(401, "U003", "이메일 또는 비밀번호가 올바르지 않습니다."),

    // 구직자/구인자
    DUPLICATE_WORKER_PROFILE(400, "W001", "이미 등록된 구직자 프로필입니다."),
    DUPLICATE_EMPLOYER_PROFILE(400, "E001", "이미 등록된 구인자 프로필입니다."),
    INVALID_ROLE(400, "R001", "역할이 올바르지 않습니다."),
    WORKER_NOT_FOUND(404, "W002", "존재하지 않는 구직자입니다."),
    EMPLOYER_NOT_FOUND(404, "E002", "구인자 프로필이 등록되지 않았습니다."),

    // 공고
    JOB_POSTING_NOT_FOUND(404, "J001", "존재하지 않는 공고입니다."),

    // 지원/매칭
    DUPLICATE_APPLICATION(400, "P001", "이미 지원한 공고입니다."),
    APPLICATION_NOT_FOUND(404, "P002", "존재하지 않는 지원입니다."),
    CAPACITY_EXCEEDED(400, "P003", "정원이 초과되었습니다."),
    MATCH_NOT_FOUND(404, "M001", "존재하지 않는 매칭입니다."),
    INVALID_MATCH_STATUS(400, "M002", "유효하지 않은 매칭 상태입니다."),

    // 근무 로그
    ALREADY_CHECKED_IN(400, "L001", "이미 체크인되었습니다."),
    ALREADY_CHECKED_OUT(400, "L002", "이미 체크아웃되었습니다."),
    CHECK_IN_REQUIRED(400, "L003", "체크인 후 체크아웃할 수 있습니다."),
    OUT_OF_RANGE(400, "L004", "근무지에서 너무 멀리 떨어져 있습니다."),

    // 패널티
    WORKER_PENALIZED(403, "X001", "패널티로 인해 지원이 제한되었습니다."),
    EMPLOYER_PENALIZED(403, "X002", "패널티로 인해 공고 등록이 제한되었습니다."),

    // QR
    INVALID_QR_TOKEN(400, "Q001", "유효하지 않은 QR 토큰입니다."),
    EXPIRED_QR_TOKEN(400, "Q002", "만료된 QR 토큰입니다."),
    QR_VERSION_MISMATCH(400, "Q003", "재발급된 QR이 아닙니다. 최신 QR을 다시 확인해주세요."),

    // 동시성
    LOCK_ACQUISITION_FAILED(409, "X003", "현재 처리 중인 요청이 있습니다. 잠시 후 다시 시도해주세요."),

    // 알림
    NOTIFICATION_NOT_FOUND(404, "N001", "존재하지 않는 알림입니다."),

    // 정산
    DUPLICATE_PAYMENT(400, "S001", "이미 정산이 완료되었습니다."),
    PAYMENT_NOT_FOUND(404, "S002", "존재하지 않는 정산입니다."),
    INVALID_PAYMENT_STATUS(400, "S003", "처리할 수 없는 정산 상태입니다.");

    private final int status;
    private final String code;
    private final String message;
}
