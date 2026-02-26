package com.strataguard.core.exception;

public class InsufficientPermissionException extends RuntimeException {
    private final String requiredPermission;

    public InsufficientPermissionException(String requiredPermission) {
        super("Insufficient permission: " + requiredPermission + " required");
        this.requiredPermission = requiredPermission;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }
}
