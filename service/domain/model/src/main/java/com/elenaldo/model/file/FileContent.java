package com.elenaldo.model.file;

import java.io.InputStream;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class FileContent {
    private FileInformation information;
    private InputStream content;
}
