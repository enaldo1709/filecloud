package com.elenaldo.model.exception;

import com.elenaldo.model.OperationResult;
import com.elenaldo.model.enums.OperationStatus;

public class OperationException extends Exception{ 
    private final transient OperationResult result;

    public OperationException(OperationResult result) {
        super(result.getMessage());
        this.result = result;
    }
    
    public OperationException(String message, Throwable cause) {
        super(message,cause);
        this.result = OperationResult.builder()
            .message(message)
            .status(OperationStatus.FAILED)
            .build();
    }

    public OperationResult getResult() {
        return result;
    }
}
