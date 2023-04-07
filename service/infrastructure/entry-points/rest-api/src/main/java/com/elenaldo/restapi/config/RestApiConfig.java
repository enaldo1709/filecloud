package com.elenaldo.restapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.elenaldo.model.file.gateways.FileStorage;
import com.elenaldo.usecase.StorageService;

@Configuration
public class RestApiConfig {
    
    @Bean
    StorageService getService(FileStorage storage) {
        return new StorageService(storage);
    }

}
