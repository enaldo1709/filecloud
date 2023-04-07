package com.elenaldo.model.file;

import com.elenaldo.model.file.enums.OperationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OperationResult {
    private OperationStatus status;
    private String message;
    private FileContent content;
}
