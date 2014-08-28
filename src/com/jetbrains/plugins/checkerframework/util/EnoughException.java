package com.jetbrains.plugins.checkerframework.util;

public class EnoughException extends RuntimeException {
    public EnoughException(String message) {
        super(message);
    }

    public EnoughException(Throwable cause) {
        super(cause);
    }
}
