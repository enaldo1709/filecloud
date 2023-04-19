package com.elenaldo.usecase;

import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;

import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.OperationResult;
import com.elenaldo.model.file.enums.OperationStatus;
import com.elenaldo.model.file.exception.FileDownloadException;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;
import com.elenaldo.model.file.gateways.FileStorage;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class StorageService {
    private final FileStorage storage;

    public Mono<OperationResult> upload(FileInformation info, InputStream data) {
        return storage.exist(info.getName())
            .flatMap(b -> b.booleanValue() 
                ? this.mapErrorResult(new FileAlreadyExistsException("File already exists on storage"))
                : storage.upload(info, data)
                    .map(i -> OperationResult.builder()
                        .status(OperationStatus.SUCCESS)
                        .message("File uploaded successfuly")
                        .build()
                    )
            ).onErrorResume(FileUploadException.class, this::mapErrorResult);
    }

    public Mono<OperationResult> download(FileInformation file) {
        return storage.download(file)
            .map(content -> OperationResult.builder()
                    .status(OperationStatus.SUCCESS)
                    .content(content)
                    .build()
            ).onErrorResume(FileNotFoundException.class, this::mapErrorResult)
            .onErrorResume(FileDownloadException.class, this::mapErrorResult)
            .onErrorResume(e -> Mono.just(
                OperationResult.builder().status(OperationStatus.FAILED).message("Internal error").build()
            ));
    }

    public Flux<FileInformation> list() {
        return storage.list();
    }


    private Mono<OperationResult> mapErrorResult(Throwable t) {
        return Mono.just(
            OperationResult.builder()
                .status(OperationStatus.FAILED)
                .message(t.getMessage())
                .build()
        );
    }
}
