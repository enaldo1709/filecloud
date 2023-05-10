package com.elenaldo.postgresql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "database.r2dbc")
public class PostgresqlConnectionProperties {
    private String database;
    private String schema;
    private String username;
    private String password;
    private String host;
    private Integer port;

}