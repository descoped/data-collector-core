package io.descoped.dc.core.executor;

import io.descoped.dc.api.error.ExecutionException;

public class WorkerException extends ExecutionException {

    public WorkerException(String message) {
        super(message);
    }

    public WorkerException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkerException(Throwable cause) {
        super(cause);
    }
}
