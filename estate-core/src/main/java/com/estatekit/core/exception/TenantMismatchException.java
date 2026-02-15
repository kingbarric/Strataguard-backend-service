package com.estatekit.core.exception;

public class TenantMismatchException extends RuntimeException {

    public TenantMismatchException() {
        super("Access denied: tenant context mismatch");
    }

    public TenantMismatchException(String message) {
        super(message);
    }
}
