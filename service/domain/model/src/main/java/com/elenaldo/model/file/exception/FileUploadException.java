package com.elenaldo.model.file.exception;

public class FileUploadException extends Exception{

    public FileUploadException(Throwable cause) {
        super("Error uploading file",cause);
    }
    
}
