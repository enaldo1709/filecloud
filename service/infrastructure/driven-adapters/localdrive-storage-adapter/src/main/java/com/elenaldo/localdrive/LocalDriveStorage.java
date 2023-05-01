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
import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.exception.FileDeleteException;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;
import com.elenaldo.model.file.gateways.BufferedFileWriter;
import com.elenaldo.model.file.gateways.FileStorage;

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
    public Mono<BufferedFileWriter> upload(FileInformation information) {
        return Mono.just(information)
            .map(FileInformation::getName)
            .flatMap(this::validateFilename)
            .map(storage::resolve)
            .map(Path::toFile)
            .onErrorMap(FileUploadException::new)
            .map(LocalBufferedFileWriter::new);
    }

    private Mono<String> validateFilename(String filename) {
        if (Objects.isNull(filename) || "".equals(filename)) 
            return Mono.error(new FileUploadException(null));
        return Mono.just(filename);
    }


    @Override
    public Flux<FileInformation> list() {
        return Mono.just(storage.toFile())
            .map(File::listFiles)
            .flatMapIterable(Arrays::asList)
            .map(f -> FileInformation.builder().name(f.getName()).build())
            .doOnError(e -> log.error("Error reading files -> ", e.getCause()))
            .onErrorMap(FileNotFoundException::new);
    }

    @Override
    public Mono<FileContent> download(FileInformation information) {
        return Mono.just(information.getName())
            .map(storage::resolve)
            .map(Path::toFile)
            .flatMap(this::readFile)
            .map(t -> FileContent.builder().content(t.getT1()).size(t.getT2().longValue()))
            .map(b -> b.information(information))
            .map(b -> b.build())
            .doOnSuccess(fc -> log.info("",fc))
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

    private void flushOldFiles() {
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
