package com.elenaldo.model.file;

import com.elenaldo.model.file.enums.OperationStatus;
import com.elenaldo.model.file.exception.OperationException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OperationResult {
    private OperationStatus status;
    private String message;
    private FileContent content;


    public Mono<OperationResult> evaluate() {
        if (OperationStatus.FAILED.equals(status)) {
            return Mono.error(new OperationException(this));
        }
        return Mono.just(this);
    }
}
