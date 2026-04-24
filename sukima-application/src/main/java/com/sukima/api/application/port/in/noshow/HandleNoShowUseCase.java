package com.sukima.api.application.port.in.noshow;

public interface HandleNoShowUseCase {

    /**
     * 노쇼 감지 시 호출된다.
     * matchId 기반으로 체크인 여부를 확인하고 노쇼 처리한다.
     */
    void handle(Long matchId);
}
