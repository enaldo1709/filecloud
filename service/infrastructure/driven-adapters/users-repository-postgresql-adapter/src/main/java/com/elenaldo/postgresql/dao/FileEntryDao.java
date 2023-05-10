package com.elenaldo.postgresql.dao;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class FileEntryDao {
    private String id;
    private Boolean directory;
    private String fileName;
    private Boolean active;
    private String parentID;
    private LocalDateTime dateModified;
    private String ownerID;
}
