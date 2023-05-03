package com.elenaldo.localdrive.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import com.elenaldo.model.file.OperationResult;
import com.elenaldo.model.file.enums.OperationStatus;
import com.elenaldo.model.file.gateways.BufferedFileWriter;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class LocalBufferedFileWriter implements BufferedFileWriter{
    private static final int BUFFER_SIZE = 10_485_760;
	private File file;
	private byte[] buffer;
	private int wrote;
	private boolean error;
	private boolean finished;
    private OperationResult result;
    private CountDownLatch latch;
	
	public LocalBufferedFileWriter(String filename) {
		file = new File(filename);
		buffer = new byte[BUFFER_SIZE];
		finished = false;
		error = false;
		wrote = 0;
        latch = new CountDownLatch(1);
        result = OperationResult.builder()
            .status(OperationStatus.PENDING)
            .message("Starting file upload...")
            .build();
	}
	
	public LocalBufferedFileWriter(File file) {
		this.file = file;
		buffer = new byte[BUFFER_SIZE];
		finished = false;
		error = false;
		wrote = 0;
        latch = new CountDownLatch(1);
        result = OperationResult.builder()
            .status(OperationStatus.PENDING)
            .message("Starting file upload...")
            .build();
	}


    @Override
    public void buffer(InputStream is) {
        if (finished) return;
		try {
			wrote += is.read(buffer, wrote, buffer.length - wrote);
			checkReadyAndWrite();
			
			if (is.available() > 0) {
				buffer(is);
			}
		} catch (IOException e) {
            finishWithError("Error reading input data", e);
		}
    }

    @Override
    public void finish() {
        if (finished)
            return;
        finished = true;
		checkReadyAndWrite();
        if (error) 
            return;
        result = OperationResult.builder()
            .status(OperationStatus.SUCCESS)
            .message("File uploaded successfully")
            .build();
		log.info("File upload complete... wrote: {} on file -> {}", file.length(), file.getName());
        latch.countDown();
    }

    @Override
    public void finishWithError(String message, Throwable cause) {
        finished = true;
        error = true;
        result = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message(message)
            .build();
        log.error(message + " -> ", cause);
        buffer = new byte[0];
        wrote = 0;
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            log.warn("Couldn't delete corrupted file -> ",e);
        }
        latch.countDown();
    }

    private void checkReadyAndWrite() {
        if (wrote == 0) 
            return;
		if (wrote < buffer.length) {
			if (!finished) 
                return;
			buffer = Arrays.copyOf(buffer, wrote);
		}
		
		int currentWrote = LocalBufferedFileWriter.write(this, buffer, file, true);
		log.debug("Wrote {} bytes in file -> {}", currentWrote, file.getName());
		buffer = new byte[finished ? 0 : BUFFER_SIZE];
		this.wrote = 0;
		
	}

    private static int write(BufferedFileWriter writer, byte[] data, File out, boolean append) {
		try (FileOutputStream writing = new FileOutputStream(out,append)) {
            writing.write(data);
            return data.length;
        } catch (IOException e) {
            writer.finishWithError("Error writing file", e);
            return 0;
        }
	}

    @Override
    public Mono<OperationResult> getResult() {
        try {
            latch.await();
            return result.evaluate();
        } catch (InterruptedException e) {
            log.warn("Error awaiting for latch", e);
            Thread.currentThread().interrupt();
            return result.evaluate();
        }
    }
}
