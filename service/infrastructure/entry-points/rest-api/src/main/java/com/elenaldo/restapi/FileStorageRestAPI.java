package com.elenaldo.restapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.elenaldo.model.file.FileContent;
import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.enums.OperationStatus;
import com.elenaldo.usecase.StorageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class FileStorageRestAPI {
    private final StorageService service;

    @PostMapping("/upload")
    public Mono<ResponseEntity<Object>> upload(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        String filename = req.getParameter("name");
        filename = Objects.nonNull(filename) ? filename : file.getOriginalFilename();

        return Mono.just(filename)
            .map(FileInformation.builder()::name)
            .map(b -> b.build())
            .zipWith(this.getReqestFileData(file))
            .flatMap(t -> service.upload(t.getT1(), t.getT2()))
            .map(res -> res.getStatus().equals(OperationStatus.SUCCESS)
                ? ResponseEntity.ok().body((Object)res)
                : ResponseEntity.badRequest().body((Object)res)
            ).onErrorResume(
                IOException.class, 
                e -> Mono.just(ResponseEntity.internalServerError().body("Error performing upload operation"))
            );
    }

    @GetMapping("/download")
    public Mono<ResponseEntity<Object>> download(HttpServletRequest req) {
        String filename =  req.getParameter("filename");
        if (Objects.isNull(filename) || "".equals(filename)) {
            return Mono.just(ResponseEntity.badRequest().body("Request param 'filename' is required"));
        }

        return Mono.just(filename)
            .map(FileInformation.builder()::name)
            .map(b -> b.build())
            .flatMap(service::download)
            .flatMap(res -> OperationStatus.SUCCESS.equals(res.getStatus())
                ? Mono.just(res.getContent())
                    .map(FileContent::getContent)
                    .flatMap(r -> this.constructResourceResponse(filename,r))
                : Mono.just(ResponseEntity.badRequest().body(res))
            );
    }

    private Mono<InputStream> getReqestFileData(MultipartFile file) {
        try {
            return Mono.just(file.getInputStream());
        } catch (IOException e) {
            return Mono.error(e);
        }
    }

    private Mono<ResponseEntity<Object>> constructResourceResponse(String filename, InputStream r) {
        try {
            return Mono.just(r.readAllBytes())
                .map(ByteArrayResource::new)
                .map(resource -> 
                    ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", filename))
                        .contentLength(resource.contentLength())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource)
                );
        } catch (IOException e) {
            return Mono.just(ResponseEntity.internalServerError().body("Error performing download operation"));
        }
    }
}
