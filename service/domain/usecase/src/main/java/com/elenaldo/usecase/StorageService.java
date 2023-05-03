package com.elenaldo.usecase;

import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.OperationResult;
import com.elenaldo.model.file.enums.OperationStatus;
import com.elenaldo.model.file.exception.FileDownloadException;
import com.elenaldo.model.file.exception.FileExistsException;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.gateways.BufferedFileWriter;
import com.elenaldo.model.file.gateways.FileStorage;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class StorageService {
    private final FileStorage storage;

    public Mono<BufferedFileWriter> upload(String filename){
        FileInformation info = FileInformation.builder().name(filename).build();
        return storage.exist(info.getName())
            .flatMap(b -> b.booleanValue() 
                ? Mono.error(new FileExistsException())
                : storage.upload(info)
            );
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
