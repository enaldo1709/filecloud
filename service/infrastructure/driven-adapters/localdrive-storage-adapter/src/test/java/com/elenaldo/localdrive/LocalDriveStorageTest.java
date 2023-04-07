package com.elenaldo.localdrive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.exception.FileDownloadException;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;


class LocalDriveStorageTest {
    private static final String ROOT_FOLDER = "./test-files";
    private static final String FILE_CONTENT = "example text for test file";
    private static final String FILE_NAME = "test.txt";

    private LocalDriveStorage storage;

    @BeforeAll
    static void preTests() throws IOException {
        Path root = Path.of(ROOT_FOLDER);
        Files.createDirectories(root);

        File testFile = root.resolve(FILE_NAME).toFile();
        FileOutputStream writing = new FileOutputStream(testFile);
        writing.write(FILE_CONTENT.getBytes());
        writing.close();
    }

    @AfterAll
    static void postTests() throws IOException {
        Path root = Path.of(ROOT_FOLDER);
        for (File file : root.toFile().listFiles()) {
            Files.delete(file.toPath());
        }
        Files.deleteIfExists(root);
    }

    @BeforeEach
    void setUp() throws IOException {
        storage = new LocalDriveStorage(ROOT_FOLDER);
    }

    @Test
    void testConstructServiceSuccess() throws IOException {
        LocalDriveStorage actual = new LocalDriveStorage(ROOT_FOLDER+"/other");
        assertNotNull(actual);
    }

    @Test
    void testConstructFailedNoAccess() throws IOException {
        Assertions.assertThatThrownBy(() -> new LocalDriveStorage("/root"))
            .isInstanceOf(FileSystemException.class)
            .hasMessageContaining("Cant access to provided storage location -> /root");
            
        Assertions.assertThatThrownBy(() -> new LocalDriveStorage("/home"))
            .isInstanceOf(FileSystemException.class)
            .hasMessageContaining("Cant access to provided storage location -> /home");
        
    }

    @Test
    void testDownloadSuccess() throws FileNotFoundException, FileDownloadException, IOException {
        FileContent content = storage.download(FileInformation.builder().name(FILE_NAME).build());
        String actual =  new String(content.getContent().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(FILE_CONTENT, actual);
    }

    @Test
    void testDownloadFailedFileNotFound() throws FileNotFoundException, FileDownloadException, IOException {
        Assertions.assertThatThrownBy(() -> storage.download(FileInformation.builder().name("FILE_NAME").build()))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining("File not found");
        
    }

    @Test
    void testExistSuccess() {
        assertTrue(storage.exist(FILE_NAME));
    }

    @Test
    void testExistFailed() {
        assertFalse(storage.exist("FILE_CONTENT"));
    }

    @Test
    void testUploadSuccess() throws FileUploadException, java.io.FileNotFoundException, IOException {
        String expectedContent = "other example file";
        storage.upload(
            FileInformation.builder().name("test2.txt").build(), 
            new ByteArrayInputStream(expectedContent.getBytes())
        );

        File probe = Path.of(ROOT_FOLDER).resolve("test2.txt").toFile();
        assertTrue(probe.exists());
        FileInputStream reading = new FileInputStream(probe);
        String actualContent = new String(reading.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(expectedContent, actualContent);
        reading.close();
    }

    @Test
    void testUploadFailedUploadError() {
        Assertions.assertThatThrownBy(
            () -> storage.upload(FileInformation.builder().name("").build(), InputStream.nullInputStream())
        ).isInstanceOf(FileUploadException.class);
    }
}
