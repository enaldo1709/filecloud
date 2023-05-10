package com.elenaldo.localdrive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.elenaldo.localdrive.util.LocalBufferedFileWriter;
import com.elenaldo.model.FileContent;
import com.elenaldo.model.FileEntry;
import com.elenaldo.model.exception.FileDeleteException;
import com.elenaldo.model.exception.FileNotFoundException;
import com.elenaldo.model.exception.FileUploadException;
import com.elenaldo.model.gateways.BufferedFileWriter;
import com.elenaldo.model.gateways.FileStorage;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Slf4j
@Repository
public class LocalDriveStorage implements FileStorage {
    private final Path storage;
    private final Path trash;
    private final int trashTTL;

    public LocalDriveStorage(
            @Value("${storage.local.folder}") String folderPath, 
            @Value("${storage.trash.ttl}") int trashTtl) throws IOException {
        File folder = new File(folderPath);

        if (!folder.exists()) {
            Files.createDirectories(folder.toPath());
        }
        if (!(folder.canRead() && folder.canWrite())) {
            throw new FileSystemException("Cant access to provided storage location -> "+ folderPath);
        }

        Path trashFolder = folder.toPath().resolve("trash");
        if (!trashFolder.toFile().exists()){
            Files.createDirectories(trashFolder);
        }

        this.storage = folder.toPath();
        this.trash = trashFolder;
        this.trashTTL = trashTtl;

        flushOldFiles();
    }

    @Override
    public Mono<Boolean> exist(String filename) {
        return Mono.fromSupplier(() -> storage.resolve(filename).toFile().exists());
    }

    @Override
    public Mono<BufferedFileWriter> upload(FileEntry information) {
        return Mono.just(information)
            .flatMap(this::validateFilename)
            .map(FileEntry::getName)
            .map(storage::resolve)
            .map(Path::toFile)
            .map(LocalBufferedFileWriter::new)
            .map(BufferedFileWriter.class::cast)
            .doOnError(e -> log.error("Error uploading file -> ", e))
            .onErrorMap(FileUploadException::new);
    }

    private Mono<FileEntry> validateFilename(FileEntry fileInformation) {
        if (Objects.isNull(fileInformation.getName()))
            return Mono.error(new FileUploadException(new NullPointerException("FileInformation.name is null")));
        if ("".equals(fileInformation.getName()) || trash.toFile().getName().equals(fileInformation.getName())) 
            return Mono.error(new FileUploadException(null));
        return Mono.just(fileInformation);
    }


    @Override
    public Flux<FileEntry> list() {
        return Mono.just(storage.toFile())
            .flatMap(f -> f.exists() ? Mono.just(f) : Mono.error(new FileNotFoundException()))
            .map(File::listFiles)
            .flatMapIterable(Arrays::asList)
            .filter(f -> !(trash.toFile().getName().equals(f.getName())))
            .map(f -> FileEntry.builder().name(f.getName()).build())
            .doOnError(e -> log.error("Error reading files -> ", e.getCause()))
            .onErrorMap(FileNotFoundException::new);
    }

    @Override
    public Mono<FileContent> download(FileEntry info) {
        return Mono.just(info.getName())
            .map(storage::resolve)
            .map(Path::toFile)
            .flatMap(this::readFile)
            .map(t -> FileContent.builder().content(t.getT1()).information(info.toBuilder().size(t.getT2()).build()))
            .map(b -> b.build())
            .doOnSuccess(fc -> log.info("File read successfully -> name: {} size: {}",
                fc.getInformation().getName(), fc.getInformation().getSize()))
            .doOnError(e -> log.error("Error reading file ->", e))
            .onErrorMap(FileNotFoundException::new);        
    }

    @Override
    public Mono<Boolean> delete(String filename) {
        return Mono.just(filename)
            .map(storage::resolve)
            .map(Path::toFile)
            .flatMap(f -> {
                try {
                    Files.move(f.toPath(), trash.resolve(filename));
                    return Mono.just(true);
                } catch (IOException e) {
                    return Mono.error(e);
                } 
            }).doOnError(e -> log.error("Error deleting file -> ", e))
            .onErrorMap(FileDeleteException::new);
    }

    private Mono<Tuple2<FileInputStream,Long>> readFile(File file) {
        try {
            return Mono.just(new FileInputStream(file)).zipWith(Mono.just(file.length())); 
        } catch (java.io.FileNotFoundException e) {
            return Mono.error(e);
        }
    }

    protected void flushOldFiles() {
        AtomicInteger deleted = new AtomicInteger(0);
        Arrays.stream(this.trash.toFile().listFiles())
            .filter(f -> Instant.now().minus(trashTTL, ChronoUnit.DAYS).isAfter(Instant.ofEpochMilli(f.lastModified())))
            .forEach(f -> {
                try {
                    Files.delete(f.toPath());
                    deleted.incrementAndGet();
                } catch (IOException e) {
                    log.error("Error deleting file {} -> {}", f.getName(), e.getMessage());
                }
            });
        log.info("Deleted {} files from trash -> trash size: {}", 
            deleted.get(), this.trash.toFile().listFiles().length);
        Executors.newScheduledThreadPool(1).schedule(this::flushOldFiles, 1, TimeUnit.HOURS);
    }

}
