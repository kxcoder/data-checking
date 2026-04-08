package com.example.datachecking.domain.exception;

public class VersionNotFoundException extends DataCheckingDomainException {
    public VersionNotFoundException(Long id) {
        super(DomainErrorCode.VERSION_NOT_FOUND, "版本不存在: " + id);
    }

    public VersionNotFoundException(String message) {
        super(DomainErrorCode.VERSION_NOT_FOUND, message);
    }
}