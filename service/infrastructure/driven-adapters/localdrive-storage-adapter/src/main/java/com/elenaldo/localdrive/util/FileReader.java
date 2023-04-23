package com.elenaldo.localdrive.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class FileReader {
	private static final int BUFFER_SIZE =  10_485_760;
    private BlockingQueue<Byte[]> readQueue;
    private final InputStream source;


    public FileReader(InputStream source, BlockingQueue<Byte[]> readQueue) {
        this.readQueue = readQueue;
        this.source = source;
    }

    public Thread asyncRead() {
        return new Thread(
            () -> {
                try {
                    while (true) {
                        if (source.available() == 0) {
                            break;
                        }
                        if (readQueue.offer(read(source))) System.gc();
                    }
                    source.close();
                } catch (IOException e) {
                    log.error("Error reading file content -> ", e);
                }
            }
        );
	}

    public Byte[] read(InputStream reading) throws IOException{
        int bufferLength = (reading.available() > BUFFER_SIZE) ? BUFFER_SIZE : reading.available();
        byte[] buffer = new byte[bufferLength];
        reading.read(buffer, 0, bufferLength);
        return FileIO.mapByteArray(buffer);
    }
}
