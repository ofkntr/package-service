package com.postnl.exception;

public class UnableToDeleteException extends IllegalStateException {

    public UnableToDeleteException(String message) {
        super(message);
    }

}
