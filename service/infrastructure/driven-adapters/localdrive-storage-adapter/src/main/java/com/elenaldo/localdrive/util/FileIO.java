package com.elenaldo.localdrive.util;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FileIO {
    private BlockingQueue<Byte[]> readQueue;
    private File file;
	private InputStream content;


	public FileIO(File output, InputStream content) {
        this.file = output;
		this.readQueue = new LinkedBlockingQueue<>();
		this.content = content;
	}

	public void writeFileAsync() {
		new FileReader(content, readQueue).asyncRead().start();
		new FileWriter(readQueue, file).asyncWrite().start();
	}

	public static Byte[] mapByteArray(byte[] arr) {
		Byte[] result = new Byte[arr.length];
		for (int i = 0; i < arr.length; i++) result[i] = Byte.valueOf(arr[i]);
		return result;
	}

	public static byte[] mapByteArray(Byte[] arr) {
		byte[] result = new byte[arr.length];
		for (int i = 0; i < arr.length; i++) result[i] = arr[i].byteValue();
		return result;
	}

}
