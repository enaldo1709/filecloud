package com.elenaldo.model.file.exception;

public class FileNotFoundException extends Exception{
    public FileNotFoundException() {
        super("File not found");
    }

    public FileNotFoundException(Throwable cause) {
        super("File not found", cause);
    }
}
