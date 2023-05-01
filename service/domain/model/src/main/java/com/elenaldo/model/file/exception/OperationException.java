package com.elenaldo.model.file.exception;

import com.elenaldo.model.file.OperationResult;

public class OperationException extends Exception{ 
    private final transient OperationResult result;

    public OperationException(OperationResult result) {
        super(result.getMessage());
        this.result = result;
    }

    public OperationResult getResult() {
        return result;
    }
}
