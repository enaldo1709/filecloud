package com.elenaldo.model.gateways;

import com.elenaldo.model.FileContent;
import com.elenaldo.model.FileEntry;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileStorage {
    public Mono<Boolean> exist(String filename);
    public Mono<BufferedFileWriter> upload(FileEntry information);
    public Flux<FileEntry> list();
    public Mono<FileContent> download(FileEntry information);
    public Mono<Boolean> delete(String filename);
}
