package com.postnl.exception;

public class UnableToUpdateException extends IllegalStateException {
    public UnableToUpdateException(String message) {
        super(message);
    }
}
