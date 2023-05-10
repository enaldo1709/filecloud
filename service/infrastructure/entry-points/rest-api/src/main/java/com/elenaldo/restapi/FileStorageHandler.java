package com.elenaldo.restapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.elenaldo.model.FileContent;
import com.elenaldo.model.FileEntry;
import com.elenaldo.model.OperationResult;
import com.elenaldo.model.enums.OperationStatus;
import com.elenaldo.model.exception.OperationException;
import com.elenaldo.usecase.StorageService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;



@Component
@RequiredArgsConstructor
public class FileStorageHandler {
    private static final int BUFFER_SIZE = 10240;
    private final StorageService service;

    @Autowired
    @Qualifier("download-url")
    private String downloadURL;


    public Mono<ServerResponse> upload(ServerRequest req) {
        return Mono.justOrEmpty(req.queryParam("filename"))
            .flatMap(service::upload)
            .flatMap(writer -> req.multipartData()
                .flatMapIterable(m -> m.get("file"))
                .map(p -> p.content()
                    .map(b -> b.asInputStream())
                    .doOnNext(writer::buffer)
                    .doOnComplete(writer::finish)
                    .subscribe()
                ).then(writer.getResult()))
            .flatMap(r -> mapResultResponse(HttpStatus.CREATED, r))
            .onErrorResume(OperationException.class, e -> mapResultResponse(HttpStatus.INTERNAL_SERVER_ERROR, e))
            .switchIfEmpty(Mono.error(new OperationException("required param 'filename' not found",null)))
            .onErrorResume(OperationException.class, e -> mapResultResponse(HttpStatus.BAD_REQUEST, e));
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        return service.list()
            .map(i -> new FileInformationDTO(i.getName(), String.format("%s?filename=%s",downloadURL, i.getName())))
            .collectList()
            .flatMap(ServerResponse.ok()::bodyValue);
    }

    public Mono<ServerResponse> download(ServerRequest req) {
        return Mono.justOrEmpty(req.queryParam("filename"))
            .map(n -> FileEntry.builder().name(n).build())
            .flatMap(service::download)
            .flatMap(OperationResult::evaluate)
            .map(OperationResult::getContent)
            .flatMap(this::mapDownloadResponse)
            .onErrorResume(OperationException.class, e -> mapResultResponse(HttpStatus.INTERNAL_SERVER_ERROR,e))
            .switchIfEmpty(ServerResponse.badRequest().bodyValue("required param 'filename' not found"));
    }

    private Mono<ServerResponse> mapDownloadResponse(FileContent fc) {
        return ServerResponse.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", fc.getInformation().getName()))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(fc.getInformation().getSize())
            .body(BodyInserters.fromDataBuffers(
                DataBufferUtils.readInputStream(fc::getContent, new DefaultDataBufferFactory(), BUFFER_SIZE)
            ));
    }

    private Mono<ServerResponse> mapResultResponse(HttpStatus status, OperationResult result) {
        return Mono.just(new OperationResultDTO(result.getStatus(), result.getMessage()))
            .flatMap(ServerResponse.status(status)::bodyValue);
    }

    private Mono<ServerResponse> mapResultResponse(HttpStatus status, OperationException exception) {
        return Mono.just(exception.getResult())
            .flatMap(or -> mapResultResponse(status, or));
    }

    private record FileInformationDTO(String name, String downloadURL){}
    private record OperationResultDTO(OperationStatus status, String message){}
}
