package com.elenaldo.model.file;

import com.elenaldo.model.file.enums.OperationStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class OperationResult {
    private OperationStatus status;
    private String message;
    private FileContent content;
}
