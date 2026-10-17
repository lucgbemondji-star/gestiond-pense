package com.depenses.util;

public class ApiException extends RuntimeException {
    private final int codeHttp;

    public ApiException(int codeHttp, String message) {
        super(message);
        this.codeHttp = codeHttp;
    }

    public ApiException(int codeHttp, String message, Throwable cause) {
        super(message, cause);
        this.codeHttp = codeHttp;
    }

    public int getCodeHttp() {
        return codeHttp;
    }
}
