package com.elenaldo.restapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.OperationResult;
import com.elenaldo.model.file.exception.OperationException;
import com.elenaldo.usecase.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
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
            .switchIfEmpty(Mono.just("file"))
            .flatMap(service::upload)
            .flatMap(writer -> req.multipartData()
                .flatMapIterable(m -> m.get("file"))
                .map(p -> p.content()
                    .map(b -> b.asInputStream())
                    .doOnNext(writer::buffer)
                    .doOnComplete(writer::finish)
                    .subscribe()
                ).then(Mono.fromSupplier(writer::getResult))
            ).flatMap(ServerResponse.ok()::bodyValue)
            .onErrorResume(OperationException.class, ServerResponse.badRequest()::bodyValue);
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        return service.list()
            .map(i -> new FileInformationDTO(i.getName(), String.format("%s?filename=%s",downloadURL, i.getName())))
            .collectList()
            .flatMap(ServerResponse.ok()::bodyValue);
    }

    public Mono<ServerResponse> download(ServerRequest req) {
        return Mono.justOrEmpty(req.queryParam("filename"))
            .map(n -> FileInformation.builder().name(n).build())
            .flatMap(service::download)
            .flatMap(OperationResult::evaluate)
            .map(OperationResult::getContent)
            .flatMap(this::mapDownloadResponse)
            .switchIfEmpty(ServerResponse.badRequest().bodyValue("required param 'filename' not found"));
    }

    private Mono<ServerResponse> mapDownloadResponse(FileContent fc) {
        return ServerResponse.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", fc.getInformation().getName()))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(fc.getSize())
            .body(BodyInserters.fromDataBuffers(
                DataBufferUtils.readInputStream(fc::getContent, new DefaultDataBufferFactory(), BUFFER_SIZE)
            ));
    }

    private record FileInformationDTO(String name, String downloadURL){}

}
