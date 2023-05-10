package com.elenaldo.model.exception;

public class FileUploadException extends Exception{

    public FileUploadException(Throwable cause) {
        super("Error uploading file",cause);
    }
    
}
