package com.sukima.api.application.port.in.application;

public interface ApplyJobUseCase {

    Long apply(Command command);

    record Command(Long jobPostingId, Long userId) {}
}
