package com.elenaldo.model.file.gateways;

import java.io.InputStream;

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;

public interface FileStorage {
    public boolean exist(String filename);
    public void upload(FileInformation information, InputStream dataStream) throws FileUploadException;
    public FileContent download(FileInformation information) throws FileNotFoundException;
}
