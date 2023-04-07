package com.elenaldo.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
            .thenReturn(Mono.just(content));

        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.SUCCESS)
            .content(content)
            .build();
        
        StepVerifier.create(service.download(FileInformation.builder().name("test").build()))
            .expectSubscription()
            .assertNext(actual -> assertEquals(expected, actual))
            .verifyComplete();
    }
    
    @Test
    void testDownloadFailedFileNotFound() throws FileNotFoundException, FileDownloadException {
        when(storage.download(any(FileInformation.class)))
            .thenReturn(Mono.error(new FileNotFoundException()));
        
        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("File not found")
            .build();

        StepVerifier.create(service.download(FileInformation.builder().name("test").build()))
            .expectSubscription()
            .assertNext(actual -> assertEquals(expected, actual))
            .verifyComplete();
    }
    
    @Test
    void testDownloadFailedDownloadException() throws FileNotFoundException, FileDownloadException {
        when(storage.download(any(FileInformation.class)))
            .thenReturn(Mono.error(new FileDownloadException()));
        
        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("Internal error during file download")
            .build();

        StepVerifier.create(service.download(FileInformation.builder().name("test").build()))
            .expectSubscription()
            .assertNext(actual -> assertEquals(expected, actual))
            .verifyComplete();
    }

    @Test
    void testUploadSuccess() throws FileUploadException {
        when(storage.exist(anyString())).thenReturn(Mono.just(false));
        when(storage.upload(any(FileInformation.class), any(InputStream.class)))
            .thenReturn(Mono.just(FileInformation.builder().build()));

        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.SUCCESS)
            .message("File uploaded successfuly")
            .build();

        StepVerifier
            .create(service.upload(FileInformation.builder().name("test").build(), InputStream.nullInputStream()))
            .expectSubscription()
            .assertNext(actual -> assertEquals(expected, actual))
            .verifyComplete();
    }

    @Test
    void testUploadFailedByFileExistsException() throws FileUploadException {
        when(storage.exist(anyString())).thenReturn(Mono.just(true));

        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("File already exists on storage")
            .build();

        StepVerifier
            .create(service.upload(FileInformation.builder().name("test").build(), InputStream.nullInputStream()))
            .expectSubscription()
            .assertNext(actual -> assertEquals(expected, actual))
            .verifyComplete();
    }

    @Test
    void testUploadFailedByUploadException() throws FileUploadException {
        when(storage.exist(anyString())).thenReturn(Mono.just(false));
        when(storage.upload(any(FileInformation.class), any(InputStream.class)))
            .thenReturn(Mono.error(new FileUploadException(null)));

        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("Error uploading file")
            .build();

        StepVerifier
            .create(service.upload(FileInformation.builder().name("test").build(), InputStream.nullInputStream()))
            .expectSubscription()   
            .assertNext(actual -> assertEquals(expected, actual))
            .verifyComplete();
    }
}
