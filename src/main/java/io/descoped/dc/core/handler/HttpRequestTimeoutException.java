package io.descoped.dc.core.handler;

import io.descoped.dc.api.error.ExecutionException;

public class HttpRequestTimeoutException extends ExecutionException {
    public HttpRequestTimeoutException(String message) {
        super(message);
    }
}
