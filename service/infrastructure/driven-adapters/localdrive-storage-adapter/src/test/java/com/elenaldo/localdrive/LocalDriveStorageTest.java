package com.elenaldo.localdrive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.exception.FileDownloadException;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;
import com.elenaldo.model.file.gateways.BufferedFileWriter;

import reactor.test.StepVerifier;


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
            if (file.isDirectory()) {
                for (File f2 : file.listFiles()) {
                    Files.delete(f2.toPath());
                }
            }
            Files.delete(file.toPath());
        }
        Files.deleteIfExists(root);
    }

    @BeforeEach
    void setUp() throws IOException {
        storage = new LocalDriveStorage(ROOT_FOLDER,30);
    }

    @Test
    void testConstructServiceSuccess() throws IOException {
        LocalDriveStorage actual = new LocalDriveStorage(ROOT_FOLDER+"/other",30);
        assertNotNull(actual);
    }

    @Test
    void testConstructFailedNoAccess() throws IOException {
        Assertions.assertThatThrownBy(() -> new LocalDriveStorage("/root",30))
            .isInstanceOf(FileSystemException.class)
            .hasMessageContaining("Cant access to provided storage location -> /root");
            
        Assertions.assertThatThrownBy(() -> new LocalDriveStorage("/home",30))
            .isInstanceOf(FileSystemException.class)
            .hasMessageContaining("Cant access to provided storage location -> /home");
        
    }

    @Test
    void testDownloadSuccess() throws FileNotFoundException, FileDownloadException, IOException {
        StepVerifier.create(storage.download(FileInformation.builder().name(FILE_NAME).build()))
            .expectSubscription()
            .assertNext(content -> {
                String actual;
                try {
                    actual = new String(content.getContent().readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    actual = null;
                }
                assertEquals(FILE_CONTENT, actual);
            }).verifyComplete();
    }

    @Test
    void testDownloadFailedFileNotFound() throws FileNotFoundException, FileDownloadException, IOException {
        StepVerifier.create(storage.download(FileInformation.builder().name("FILE_NAME").build()))
            .expectError(FileNotFoundException.class)
            .verify();
    }

    @Test
    void testExistSuccess() {
        StepVerifier.create(storage.exist(FILE_NAME))
            .expectSubscription()
            .assertNext(actual -> assertTrue(actual))
            .verifyComplete();
    }

    @Test
    void testExistFailed() {
        StepVerifier.create(storage.exist("FILE_NAME"))
            .expectSubscription()
            .assertNext(actual -> assertFalse(actual))
            .verifyComplete();
    }

    @Test
    void testUploadSuccess() {
        //String expectedContent = "other example file";
        StepVerifier.create(storage.upload(FileInformation.builder().name("test2.txt").build()))
        .expectSubscription()
        // .assertNext(v -> {
        //     File probe = Path.of(ROOT_FOLDER).resolve("test2.txt").toFile();
        //     assertTrue(probe.exists());
        //     String actualContent;
        //     try (FileInputStream reading = new FileInputStream(probe)) {
        //         actualContent = new String(reading.readAllBytes(), StandardCharsets.UTF_8);
        //         reading.close();
        //     } catch (IOException e) {
        //         actualContent = null;
        //     }
        //     assertEquals(expectedContent, actualContent);
        // })
        .assertNext(actual -> assertInstanceOf(BufferedFileWriter.class, actual, "is instance of BufferedFileWriter"))
        .verifyComplete();
    }

    @Test
    void testUploadFailedUploadError() {
        StepVerifier.create(storage.upload(FileInformation.builder().name("").build()))
            .expectError(FileUploadException.class)
            .verify();
    }

    @Test
    void testDeleteSuccess() throws IOException {
        preTests();
        StepVerifier.create(storage.delete(FILE_NAME))
            .expectSubscription()
            .assertNext(b -> assertTrue(b))
            .verifyComplete();

    }
    
    @Test
    void testDeleteFailed() {

    }
}
