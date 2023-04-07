package com.elenaldo.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.OperationResult;
import com.elenaldo.model.file.enums.OperationStatus;
import com.elenaldo.model.file.exception.FileDownloadException;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;
import com.elenaldo.model.file.gateways.FileStorage;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {
    @Mock
    private FileStorage storage;

    @InjectMocks
    private StorageService service;


    @Test
    void testDownloadSuccess() throws FileNotFoundException, FileDownloadException {
        FileContent content = FileContent.builder().build();
        
        when(storage.download(any(FileInformation.class)))
            .thenReturn(content);

        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.SUCCESS)
            .content(content)
            .build();
        
        OperationResult actual = service.download(FileInformation.builder().name("test").build());
        assertEquals(expected, actual);
    }
    
    @Test
    void testDownloadFailedFileNotFound() throws FileNotFoundException, FileDownloadException {
        when(storage.download(any(FileInformation.class)))
            .thenThrow(new FileNotFoundException());
        
        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("File not found")
            .build();

        OperationResult actual = service.download(FileInformation.builder().name("test").build());
        assertEquals(expected, actual);
    }
    
    @Test
    void testDownloadFailedDownloadException() throws FileNotFoundException, FileDownloadException {
        when(storage.download(any(FileInformation.class)))
            .thenThrow(new FileDownloadException());
        
        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("Internal error during file download")
            .build();

        OperationResult actual = service.download(FileInformation.builder().name("test").build());
        assertEquals(expected, actual);
    }

    @Test
    void testUploadSuccess() throws FileUploadException {
        when(storage.exist(anyString())).thenReturn(false);
        doNothing().when(storage).upload(isA(FileInformation.class), isA(InputStream.class));

        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.SUCCESS)
            .message("File uploaded successfuly")
            .build();

        OperationResult actual = service.upload(
            FileInformation.builder().name("test").build(), InputStream.nullInputStream());
        
        assertEquals(expected, actual);
    }

    @Test
    void testUploadFailedByFileExistsException() throws FileUploadException {
        when(storage.exist(anyString())).thenReturn(true);

        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("File already exists on storage")
            .build();

        OperationResult actual = service.upload(
            FileInformation.builder().name("test").build(), InputStream.nullInputStream());
        
        assertEquals(expected, actual);
    }

    @Test
    void testUploadFailedByUploadException() throws FileUploadException {
        when(storage.exist(anyString())).thenReturn(false);
        doThrow(new FileUploadException(null))
            .when(storage).upload(isA(FileInformation.class), isA(InputStream.class));

        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("Error uploading file")
            .build();

        OperationResult actual = service.upload(
            FileInformation.builder().name("test").build(), InputStream.nullInputStream());
        
        assertEquals(expected, actual);
    }
}
