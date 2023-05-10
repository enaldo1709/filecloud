package com.elenaldo.model;

import com.elenaldo.model.enums.OperationStatus;
import com.elenaldo.model.exception.OperationException;

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
