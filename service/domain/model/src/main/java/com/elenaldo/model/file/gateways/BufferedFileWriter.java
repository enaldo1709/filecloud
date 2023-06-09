package com.elenaldo.model.file.gateways;

import java.io.InputStream;

import com.elenaldo.model.file.OperationResult;

import reactor.core.publisher.Mono;

public interface BufferedFileWriter {
    public void buffer(InputStream is);
    public void finish();
    public void finishWithError(String message, Throwable cause);
    public Mono<OperationResult> getResult();
}
