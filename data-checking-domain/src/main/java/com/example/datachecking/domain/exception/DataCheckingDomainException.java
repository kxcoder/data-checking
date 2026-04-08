package com.example.datachecking.domain.exception;

public class DataCheckingDomainException extends RuntimeException {
    private final DomainErrorCode errorCode;

    public DataCheckingDomainException(DomainErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public DataCheckingDomainException(DomainErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public DataCheckingDomainException(DomainErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public DataCheckingDomainException(DomainErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }

    public DomainErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorCodeStr() {
        return errorCode.getCode();
    }
}