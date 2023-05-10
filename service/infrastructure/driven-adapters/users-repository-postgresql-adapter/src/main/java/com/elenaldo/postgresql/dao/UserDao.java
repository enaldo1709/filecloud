package com.elenaldo.postgresql.dao;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class UserDao {
    private String username;
    private String email;
    private String personalName;
    private String userPassword;
    private LocalDateTime dateCreated;
    private String publicKey;
    private String privateKey;
}
