package com.elenaldo.localdrive.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.elenaldo.model.enums.OperationStatus;
import com.elenaldo.model.exception.OperationException;

import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LocalBufferedFileWriterTest {
    private static final String ROOT_FOLDER = "./test-files-buff";
    private static final String FILE_CONTENT = "example text for test file";
    private static final String FILE_NAME = "test.txt";

    @BeforeEach
    void preTests() throws IOException {
        Files.createDirectories(Path.of(ROOT_FOLDER));
    }

    @AfterEach
    void postTests() throws IOException {
        deleteFile(Path.of(ROOT_FOLDER).toFile());
    }

    private void deleteFile(File file) throws IOException {
        if (file.isDirectory()) {
            for (File f2 : file.listFiles()) {
                deleteFile(f2);
            }
        }
        Files.deleteIfExists(file.toPath());
    }

    @Test
    void testBuffer() throws IOException {
        File testFile = Path.of(ROOT_FOLDER).resolve(FILE_NAME).toFile();
        LocalBufferedFileWriter writer = new LocalBufferedFileWriter(testFile);

        writer.buffer(new ByteArrayInputStream(FILE_CONTENT.getBytes(StandardCharsets.UTF_8)));
        writer.finish();

        assertTrue(testFile.exists());
        String content;
        try (FileInputStream testing = new FileInputStream(testFile)) {
            content = new String(testing.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            content = null;
        }

        assertEquals(FILE_CONTENT, content);

        StepVerifier.create(writer.getResult())
            .expectSubscription()
            .assertNext(result -> assertEquals(OperationStatus.SUCCESS, result.getStatus()))
            .verifyComplete();

        byte[] testContent = new byte[10_485_760 * 3];
        testFile.delete();

        writer = new LocalBufferedFileWriter(testFile);
        writer.buffer(new ByteArrayInputStream(testContent));
        writer.finish();

        assertTrue(testFile.exists());
        assertEquals(10_485_760 * 3, testFile.length());
    }

    @Test
    void testBufferFailed() throws IOException {
        InputStream data = mock(InputStream.class);
        when(data.read(any(), anyInt(), anyInt())).thenThrow(new IOException("Error"));

        LocalBufferedFileWriter newWriter = new LocalBufferedFileWriter("test");
        newWriter.buffer(data);

        StepVerifier.create(newWriter.getResult())
            .expectErrorSatisfies(e -> {
                assertTrue(e instanceof OperationException);
                OperationException e1 = (OperationException)e;
                assertEquals(OperationStatus.FAILED,e1.getResult().getStatus());
            }).verify();
    }

    @Test
    void testWriteFailed() throws IOException {
        File temp = File.listRoots()[0].toPath().resolve("tmp-file").toFile();

        LocalBufferedFileWriter newWriter = new LocalBufferedFileWriter(temp);
        newWriter.buffer(new ByteArrayInputStream(FILE_CONTENT.getBytes(StandardCharsets.UTF_8))); 
        newWriter.finish();

        StepVerifier.create(newWriter.getResult())
            .expectErrorSatisfies(e -> {
                assertTrue(e instanceof OperationException);
                OperationException e1 = (OperationException)e;
                assertEquals(OperationStatus.FAILED,e1.getResult().getStatus());
            }).verify();
    }

    @Test
    void testFinish() {
        LocalBufferedFileWriter writer = new LocalBufferedFileWriter(FILE_NAME);
        writer.finish();

        writer.buffer(new ByteArrayInputStream(FILE_CONTENT.getBytes(StandardCharsets.UTF_8)));

        File testFile = new File(FILE_NAME);
        
        assertFalse(testFile.exists());

        final LocalBufferedFileWriter newWriter = new LocalBufferedFileWriter("test");
        ReflectionTestUtils.setField(newWriter, "finished", true);
        assertDoesNotThrow(() -> newWriter.finish(), "try to finish when finished");
    }

    @Test
    void testFinishWithError() throws IOException {
        File testFile = Path.of(ROOT_FOLDER).resolve(FILE_NAME).toFile();
        LocalBufferedFileWriter writer = new LocalBufferedFileWriter(testFile);

        byte[] testContent = new byte[10_485_760 * 3];

        writer.buffer(new ByteArrayInputStream(testContent));
        writer.finishWithError("Error writing file", null);
        writer.buffer(new ByteArrayInputStream(testContent));
        
        StepVerifier.create(writer.getResult())
            .expectErrorSatisfies(e -> {
                assertTrue(e instanceof OperationException);
                OperationException e1 = (OperationException)e;
                assertEquals(OperationStatus.FAILED,e1.getResult().getStatus());
                assertEquals("Error writing file", e1.getResult().getMessage());
            }).verify();
        assertFalse(testFile.exists());
        
        writer = new LocalBufferedFileWriter(testFile);
        writer.finishWithError("Error creating file", null);
        writer.buffer(new ByteArrayInputStream(testContent));

        StepVerifier.create(writer.getResult())
            .expectErrorSatisfies(e -> {
                assertTrue(e instanceof OperationException);
                OperationException e1 = (OperationException)e;
                assertEquals(OperationStatus.FAILED, e1.getResult().getStatus());
                assertEquals("Error creating file", e1.getResult().getMessage());
            }).verify();

        assertFalse(testFile.exists());

        File testDir = Path.of(ROOT_FOLDER).resolve("test-dir").toFile();
        Files.createDirectories(testDir.toPath());
        File temp = Files.createTempFile(testDir.toPath(), FILE_NAME, "").toFile();

        writer = new LocalBufferedFileWriter(temp);
        writer.buffer(new ByteArrayInputStream(FILE_CONTENT.getBytes(StandardCharsets.UTF_8)));
        writer.finish();

        assertTrue(temp.exists());

        writer = new LocalBufferedFileWriter(testDir);
        writer.finishWithError("Error writing file", null);

        assertTrue(testDir.exists());

        final LocalBufferedFileWriter newWriter = new LocalBufferedFileWriter("test");
        ReflectionTestUtils.setField(newWriter, "finished", true);
        assertDoesNotThrow(
            () -> newWriter.finishWithError("Error", null), 
            "try to finish with error when finished"
        );
    }

    @Test
    void testGetResult() throws InterruptedException {
        CountDownLatch latch = mock(CountDownLatch.class);
        doThrow(new InterruptedException()).when(latch).await();

        File testFile = Path.of(ROOT_FOLDER).resolve(FILE_NAME).toFile();
        LocalBufferedFileWriter writer = new LocalBufferedFileWriter(testFile);

        ReflectionTestUtils.setField(writer, "latch", latch);
        
        writer.buffer(new ByteArrayInputStream(FILE_CONTENT.getBytes(StandardCharsets.UTF_8)));
        StepVerifier.create(writer.getResult())
            .expectSubscription()
            .assertNext(result -> assertEquals(OperationStatus.PENDING, result.getStatus()))
            .verifyComplete();

        writer.finish();
    }
}
