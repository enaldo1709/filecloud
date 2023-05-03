package com.elenaldo.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

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
import com.elenaldo.model.file.exception.FileExistsException;
import com.elenaldo.model.file.exception.FileNotFoundException;
import com.elenaldo.model.file.exception.FileUploadException;
import com.elenaldo.model.file.gateways.BufferedFileWriter;
import com.elenaldo.model.file.gateways.FileStorage;

import reactor.core.publisher.Flux;
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
    void testDownloadFailedException() throws FileNotFoundException, FileDownloadException {
        when(storage.download(any(FileInformation.class)))
            .thenReturn(Mono.error(new IOException()));
        
        OperationResult expected = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("Internal error")
            .build();

        StepVerifier.create(service.download(FileInformation.builder().name("test").build()))
            .expectSubscription()
            .assertNext(actual -> assertEquals(expected, actual))
            .verifyComplete();
    }

    @Test
    void testUploadSuccess() throws FileUploadException {
        BufferedFileWriter writer = mock(BufferedFileWriter.class);

        when(storage.exist(anyString())).thenReturn(Mono.just(false));
        when(storage.upload(any(FileInformation.class))).thenReturn(Mono.just(writer));

        StepVerifier
            .create(service.upload("test"))
            .expectSubscription()
            .assertNext(actual -> assertEquals(writer, actual))
            .verifyComplete();
    }

    @Test
    void testUploadFailedByFileExistsException() throws FileUploadException {
        when(storage.exist(anyString())).thenReturn(Mono.just(true));

        StepVerifier
            .create(service.upload("test"))
            .expectError(FileExistsException.class)
            .verify();
    }

    @Test
    void testUploadFailedByUploadException() throws FileUploadException {
        when(storage.exist(anyString())).thenReturn(Mono.just(false));
        when(storage.upload(any(FileInformation.class)))
            .thenReturn(Mono.error(new FileUploadException(null)));


        StepVerifier
            .create(service.upload("test"))
            .expectError(FileUploadException.class)
            .verify();
    }

    @Test
    void testList() {
        when(storage.list()).thenReturn(Flux.just(
            FileInformation.builder().name("file1").build(),
            FileInformation.builder().name("file2").build()
        ));

        StepVerifier.create(service.list())
            .expectSubscription()
            .assertNext(fi -> assertEquals("file1", fi.getName()))
            .assertNext(fi -> assertEquals("file2", fi.getName()))
            .verifyComplete();
            
    }
}
