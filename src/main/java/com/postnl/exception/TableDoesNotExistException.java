package com.postnl.exception;

public class TableDoesNotExistException extends IllegalStateException {
    public TableDoesNotExistException(String message) {
        super(message);
    }
}
