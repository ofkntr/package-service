package com.postnl.exception;

public class ProductDoesNotExistException extends IllegalArgumentException {

    public ProductDoesNotExistException(String message) {
        super(message);
    }
}
