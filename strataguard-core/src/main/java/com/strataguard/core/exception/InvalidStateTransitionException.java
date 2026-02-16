package com.strataguard.core.exception;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String resourceName, String currentState, String targetState) {
        super(String.format("Cannot transition %s from '%s' to '%s'", resourceName, currentState, targetState));
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
