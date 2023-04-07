package com.elenaldo.model.file.exception;

public class FileDownloadException extends Exception{
    public FileDownloadException() {
        super("Internal error during file download");
    }
    
    public FileDownloadException(Throwable cause) {
        super("Internal error during file download",cause);
    }
}
