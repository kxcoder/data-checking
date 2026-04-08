package com.example.datachecking.domain.exception;

public class RecordNotFoundException extends DataCheckingDomainException {
    public RecordNotFoundException(Long id) {
        super(DomainErrorCode.RECORD_NOT_FOUND, "记录不存在: " + id);
    }

    public RecordNotFoundException(String message) {
        super(DomainErrorCode.RECORD_NOT_FOUND, message);
    }
}