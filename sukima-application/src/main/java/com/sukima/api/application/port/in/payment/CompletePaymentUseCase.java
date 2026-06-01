package com.sukima.api.application.port.in.payment;

public interface CompletePaymentUseCase {

    void complete(Long paymentId);

    void fail(Long paymentId);
}
