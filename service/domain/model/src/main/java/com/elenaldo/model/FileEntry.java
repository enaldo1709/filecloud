package com.elenaldo.model;

import java.time.LocalDateTime;

import com.elenaldo.model.enums.FileType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FileEntry {
    private String id;
    private FileType type;
    private String name;
    private Boolean active;
    private FileEntry parent;
    private LocalDateTime modified;
    private String owner;
    private Long size;
}
