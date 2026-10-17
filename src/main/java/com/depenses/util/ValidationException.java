package com.depenses.util;

public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(400, message);
    }
}
