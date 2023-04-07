package com.elenaldo.model.file.exception;

public class FileExistsException extends Exception{

    public FileExistsException() {
        super("File already exists");
    }

    public FileExistsException(Throwable cause) {
        super("File already exists", cause);
    }
    
}
