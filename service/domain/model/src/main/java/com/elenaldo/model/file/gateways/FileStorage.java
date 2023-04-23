package com.elenaldo.model.file.gateways;

import java.io.InputStream;

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileStorage {
    public Mono<Boolean> exist(String filename);
    public Mono<FileInformation> upload(FileInformation information, InputStream dataStream);
    public Flux<FileInformation> list();
    public Mono<FileContent> download(FileInformation information);
    public Mono<Boolean> delete(String filename);
}
