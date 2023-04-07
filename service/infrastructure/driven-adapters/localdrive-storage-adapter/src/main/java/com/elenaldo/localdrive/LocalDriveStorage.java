package com.elenaldo.localdrive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;
import com.elenaldo.model.file.gateways.FileStorage;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public class LocalDriveStorage implements FileStorage {
    private final Path storage;

    public LocalDriveStorage(@Value("${storage.local.folder}") String folderPath) throws IOException {
        File folder = new File(folderPath);

        if (!folder.exists()) {
            Files.createDirectories(folder.toPath());
        }
        if (!(folder.canRead() && folder.canWrite())) {
            throw new FileSystemException("Cant access to provided storage location -> "+ folderPath);
        }

        this.storage = folder.toPath();
    }

    @Override
    public Mono<Boolean> exist(String filename) {
        return Mono.fromSupplier(() -> storage.resolve(filename).toFile().exists());
    }

    @Override
    public Mono<FileInformation> upload(FileInformation information, InputStream dataStream) {
        return Mono.just(information.getName())
            .map(storage::resolve)
            .map(Path::toFile)
            .flatMap(f -> this.writeFile(f, dataStream))
            .map(f -> information)
            .doOnError(e -> log.error("Error writing file -> ", e.getCause()))
            .onErrorMap(FileUploadException::new);
    }

    @Override
    public Mono<FileContent> download(FileInformation information) {
        return Mono.just(information.getName())
            .map(storage::resolve)
            .map(Path::toFile)
            .flatMap(this::readFile)
            .map(FileContent.builder()::content)
            .map(b -> b.information(information))
            .map(b -> b.build())
            .doOnError(e -> log.error("Error reading file ->", e))
            .onErrorMap(FileNotFoundException::new);        
    }

    private Mono<File> writeFile(File file, InputStream dataSource) {
        return Mono.fromRunnable(() -> {
                try (FileOutputStream writing = new FileOutputStream(file)) {
                    writing.write(dataSource.readAllBytes());
                } catch (IOException e) {
                    ReflectionUtils.rethrowRuntimeException(e);
                }
            }).then(Mono.just(file));
        
    }

    private Mono<FileInputStream> readFile(File file) {
        try {
            return Mono.just(new FileInputStream(file)); 
        } catch (java.io.FileNotFoundException e) {
            return Mono.error(e);
        }
    }
}
