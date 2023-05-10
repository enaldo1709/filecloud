package com.elenaldo.model;

import java.io.InputStream;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class FileContent {
    private FileEntry information;
    private InputStream content;
}
