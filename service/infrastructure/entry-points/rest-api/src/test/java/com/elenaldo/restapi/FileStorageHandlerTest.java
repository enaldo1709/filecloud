package com.elenaldo.restapi;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.reactive.function.server.ServerRequest;

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.OperationResult;
import com.elenaldo.model.file.enums.OperationStatus;
import com.elenaldo.model.file.gateways.BufferedFileWriter;
import com.elenaldo.usecase.StorageService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FileStorageHandlerTest {

    @Mock
    private StorageService service;

    @Mock
    private ServerRequest request;

    @InjectMocks
    private FileStorageHandler handler;

    @Test
    void testDownload() {


        String content = "Content of the test file";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        OperationResult result = OperationResult.builder()
            .status(OperationStatus.SUCCESS)
            .content(
                FileContent.builder()
                    .information(FileInformation.builder().name("test-file").build())
                    .content(is)
                    .size(content.length())
                    .build()
            ).message("File downloaded successfully")
            .build();

        when(service.download(any(FileInformation.class))).thenReturn(Mono.just(result));
        when(request.queryParam("filename")).thenReturn(Optional.of("test-file"));
        
        StepVerifier.create(handler.download(request))
            .expectSubscription()
            .assertNext(res -> assertTrue(res.statusCode().is2xxSuccessful()))
            .verifyComplete();

        result = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .content(
                FileContent.builder()
                    .information(FileInformation.builder().name("test-file").build())
                    .content(InputStream.nullInputStream())
                    .size(0)
                    .build()
            ).message("Error downloading file")
            .build();
        when(service.download(any(FileInformation.class))).thenReturn(Mono.just(result));
        StepVerifier.create(handler.download(request))
            .expectSubscription()
            .assertNext(res -> assertTrue(res.statusCode().is5xxServerError()))
            .verifyComplete();

        when(request.queryParam("filename")).thenReturn(Optional.empty());
        StepVerifier.create(handler.download(request))
            .expectSubscription()
            .assertNext(res -> assertTrue(res.statusCode().is4xxClientError()))
            .verifyComplete();
    }

    @Test
    void testList() {
        when(service.list()).thenReturn(Flux.just(
            FileInformation.builder().name("file1").build(),
            FileInformation.builder().name("file3").build(),
            FileInformation.builder().name("file2").build()
        ));

        StepVerifier.create(handler.list(request))
            .expectSubscription()
            .assertNext(res -> assertTrue(res.statusCode().is2xxSuccessful()))
            .verifyComplete();

        when(service.list()).thenReturn(Flux.empty());
        StepVerifier.create(handler.list(request))
            .expectSubscription()
            .assertNext(res -> assertTrue(res.statusCode().is2xxSuccessful()))
            .verifyComplete();
    }

    @Test
    void testUpload() {
        Part filePart = mock(Part.class);
        DataBuffer buffer = mock(DataBuffer.class);
        MultiValueMap<String, Part> multipartData = new MultiValueMapAdapter<>(
            Map.of("file",new ArrayList<>(List.of(filePart)))
        );

        OperationResult result = OperationResult.builder()
            .status(OperationStatus.SUCCESS)
            .message("File uploaded successfully")
            .build();

        BufferedFileWriter writer = mock(BufferedFileWriter.class);
        when(service.upload(anyString())).thenReturn(Mono.just(writer));
        when(writer.getResult()).thenReturn(result.evaluate());

        when(request.queryParam("filename")).thenReturn(Optional.of("test-file"));
        when(request.multipartData()).thenReturn(Mono.just(multipartData));
        when(filePart.content()).thenReturn(Flux.just(buffer));
        when(buffer.asInputStream()).thenReturn(InputStream.nullInputStream());

        StepVerifier.create(handler.upload(request))
            .expectSubscription()
            .assertNext(res -> assertTrue(res.statusCode().is2xxSuccessful()))
            .verifyComplete();

        result = OperationResult.builder()
            .status(OperationStatus.FAILED)
            .message("File upload failed")
            .build();
        when(writer.getResult()).thenReturn(result.evaluate());
        StepVerifier.create(handler.upload(request))
            .expectSubscription()
            .assertNext(res -> assertTrue(res.statusCode().is5xxServerError()))
            .verifyComplete();
        when(request.queryParam("filename")).thenReturn(Optional.empty());
        StepVerifier.create(handler.upload(request))
            .expectSubscription()
            .assertNext(res -> assertTrue(res.statusCode().is4xxClientError()))
            .verifyComplete();
    }
}
