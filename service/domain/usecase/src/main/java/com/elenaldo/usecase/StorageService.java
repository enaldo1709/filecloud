package com.elenaldo.usecase;

import java.io.InputStream;

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.OperationResult;
import com.elenaldo.model.file.enums.OperationStatus;
import com.elenaldo.model.file.exception.FileDownloadException;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;
import com.elenaldo.model.file.gateways.FileStorage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StorageService {
    private final FileStorage storage;

    public OperationResult upload(FileInformation info, InputStream data) {
        if (storage.exist(info.getName())) {
            return OperationResult.builder()
                .status(OperationStatus.FAILED)
                .message("File already exists on storage")
                .build();
        }
        try {
            storage.upload(info, data);
            return OperationResult.builder()
                .status(OperationStatus.SUCCESS)
                .message("File uploaded successfuly")
                .build();
        } catch (FileUploadException e) {
            return OperationResult.builder()
                .status(OperationStatus.FAILED)
                .message(e.getMessage())
                .build();
        }
        
    }

    public OperationResult download(FileInformation file) {
        try {
            FileContent content = storage.download(file);
            return OperationResult.builder()
                .status(OperationStatus.SUCCESS)
                .content(content)
                .build();
        } catch (FileNotFoundException | FileDownloadException e) {
            return OperationResult.builder()
                .status(OperationStatus.FAILED)
                .message(e.getMessage())
                .build();
        }
    }
}
