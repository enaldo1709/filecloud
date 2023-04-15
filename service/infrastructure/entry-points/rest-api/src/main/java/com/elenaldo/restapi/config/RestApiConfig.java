package com.elenaldo.restapi.config;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.elenaldo.model.file.gateways.FileStorage;
import com.elenaldo.usecase.StorageService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RestApiConfig {
    
    @Bean
    StorageService getService(FileStorage storage) {
        return new StorageService(storage);
    }

    @Bean
    @Qualifier("download-url")
    String getDownload(
        @Value("${server.servlet.context-path}") String contextPath,
        @Value("${server.port}") Integer port,
        @Value("${storage.host}") String hostIP
    ) throws UnknownHostException {
        String ip = hostIP;
        if (Objects.isNull(hostIP) || "null".equals(hostIP)) {
            ip = Inet4Address.getLocalHost().getHostAddress();
        }

        log.info("ip -> {}",ip);
        return String.format("http://%s:%d%s/download", ip , port, contextPath);
    }
}
