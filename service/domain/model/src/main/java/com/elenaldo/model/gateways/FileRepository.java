package com.elenaldo.model.gateways;

import com.elenaldo.model.FileEntry;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileRepository {
    public Mono<FileEntry> get(String fileID);
    public Mono<FileEntry> getByIDAndUserID(String fileID, String username);
    public Flux<FileEntry> list(String parentID);
    public Mono<FileEntry> create(FileEntry user);
    public Mono<FileEntry> update(FileEntry user);
    public Mono<Boolean> delete(String fileID);
}
