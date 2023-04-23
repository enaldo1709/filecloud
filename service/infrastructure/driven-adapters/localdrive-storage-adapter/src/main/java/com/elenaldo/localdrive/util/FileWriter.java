package com.elenaldo.localdrive.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileWriter {
    private BlockingQueue<Byte[]> writeQueue;
    private File file;
    
    public FileWriter(BlockingQueue<Byte[]> writeQueue, File output) {
        this.file = output;
        this.writeQueue = writeQueue;
    }

    public Thread asyncWrite() {
        return new Thread(
            () -> {
                try (OutputStream writing = new FileOutputStream(file)) {
                    int timeout = 5;
                    Instant now = Instant.now();
                    int bytesWrote = 0;
                    while (true) {
                        Byte[] element = writeQueue.poll(timeout, TimeUnit.SECONDS);
                        timeout = 0;
                        if (Objects.isNull(element)) {
                            break;
                        }
                        bytesWrote += write(writing, element);
                        element = null;
                        System.gc();
                    }
                    long elapsed = Instant.now().minusMillis(now.toEpochMilli()).toEpochMilli();
                    log.info("File {} uploaded successfully -> wrote: {} bytes in {} ms.",file.getName(),bytesWrote,elapsed);
                } catch (IOException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                }   
            }
        );
	}

    private int write(OutputStream output, Byte[] buffer) throws IOException {
        output.write(FileIO.mapByteArray(buffer), 0, buffer.length);
        return buffer.length;
    }
}
