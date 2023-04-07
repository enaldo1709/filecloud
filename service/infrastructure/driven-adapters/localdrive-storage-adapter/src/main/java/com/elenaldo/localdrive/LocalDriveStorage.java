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

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;
import com.elenaldo.model.file.gateways.FileStorage;

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
    public boolean exist(String filename) {
        return storage.resolve(filename).toFile().exists();
    }

    @Override
    public void upload(FileInformation information, InputStream dataStream) throws FileUploadException {
        File uploading = storage.resolve(information.getName()).toFile();

        try (FileOutputStream writing = new FileOutputStream(uploading)) {
            writing.write(dataStream.readAllBytes());
        } catch (IOException e) {
            throw new FileUploadException(e);
        }
    }

    @Override
    public FileContent download(FileInformation information) throws FileNotFoundException {
        File downloading = storage.resolve(information.getName()).toFile();
        try {
            FileInputStream reading = new FileInputStream(downloading);
            return FileContent.builder()
                .information(information)
                .content(reading)
                .build();
        } catch (java.io.FileNotFoundException e) {
            throw new FileNotFoundException(e);
        }
    }
    
}
