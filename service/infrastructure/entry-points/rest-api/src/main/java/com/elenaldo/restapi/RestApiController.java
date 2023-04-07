package com.elenaldo.restapi;

import java.io.IOException;
import java.util.Objects;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.elenaldo.model.file.FileInformation;
import com.elenaldo.model.file.OperationResult;
import com.elenaldo.model.file.enums.OperationStatus;
import com.elenaldo.usecase.StorageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RestApiController {
    private final StorageService service;

    @PostMapping("/upload")
    public ResponseEntity<Object> upload(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        OperationResult result = null;
        String filename = req.getParameter("filename");
        filename = Objects.nonNull(filename) ? filename : file.getOriginalFilename();
        FileInformation information = FileInformation.builder().name(filename).build();
        try {
            result = service.upload(information, file.getInputStream());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error performing upload operation");
        }

        if (Objects.isNull(result)) {
            return ResponseEntity.internalServerError().body("Error performing upload operation");
        }

        if (result.getStatus().equals(OperationStatus.FAILED)) {
            return ResponseEntity.badRequest().body(result);
        }
        
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/download")
    public ResponseEntity<Object> download(HttpServletRequest req) {
        String filename =  req.getParameter("name");
        if (Objects.isNull(filename) || "".equals(filename)) {
            return ResponseEntity.badRequest().body("Request param 'filename' is required");
        }

        OperationResult result = service.download(FileInformation.builder().name(filename).build());

        if (Objects.isNull(result)) {
            return ResponseEntity.internalServerError().body("Error performing download operation");
        }
        if (result.getStatus().equals(OperationStatus.FAILED)) {
            return ResponseEntity.badRequest().body(result);
        }

        try {
            Resource resource = new ByteArrayResource(result.getContent().getContent().readAllBytes());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+filename+"\"")
                .contentLength(resource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error performing download operation");
        }
    }
}
