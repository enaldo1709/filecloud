package com.elenaldo.model;

import java.security.KeyPair;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class User {
    private String username;
    private String email;
    private String name;
    private String password;
    private LocalDateTime created;
    private KeyPair keys;
}
