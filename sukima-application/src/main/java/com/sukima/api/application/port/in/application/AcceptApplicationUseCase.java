package com.sukima.api.application.port.in.application;

public interface AcceptApplicationUseCase {

    Long accept(Command command);

    record Command(Long applicationId, Long employerId) {}
}
