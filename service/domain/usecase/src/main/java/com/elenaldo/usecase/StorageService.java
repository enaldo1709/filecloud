package com.elenaldo.usecase;

import com.elenaldo.model.FileEntry;
import com.elenaldo.model.OperationResult;
import com.elenaldo.model.enums.OperationStatus;
import com.elenaldo.model.exception.FileDownloadException;
import com.elenaldo.model.exception.FileExistsException;
import com.elenaldo.model.exception.FileNotFoundException;
import com.elenaldo.model.gateways.BufferedFileWriter;
import com.elenaldo.model.gateways.FileStorage;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class StorageService {
    private final FileStorage storage;

    public Mono<BufferedFileWriter> upload(String filename){
        FileEntry info = FileEntry.builder().name(filename).build();
        return storage.exist(info.getName())
            .flatMap(b -> b.booleanValue() 
                ? Mono.error(new FileExistsException())
                : storage.upload(info)
            );
    }

    public Mono<OperationResult> download(FileEntry file) {
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

    public Flux<FileEntry> list() {
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
