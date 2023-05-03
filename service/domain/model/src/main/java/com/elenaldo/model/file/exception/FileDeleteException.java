package com.elenaldo.model.file.exception;

public class FileDeleteException extends Exception{
    public FileDeleteException() {
        super("Error deleting file");
    }
    public FileDeleteException(Throwable cause) {
        super("Error deleting file",cause);
    }
}
