package com.elenaldo.restapi.config;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.elenaldo.model.file.gateways.FileStorage;
import com.elenaldo.restapi.FileStorageHandler;
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
            ip = InetAddress.getLocalHost().getHostAddress();
        }

        log.info("Local host ip -> {}",ip);
        return String.format("http://%s:%d%s/download", ip , port, contextPath);
    }

    @Bean
    RouterFunction<ServerResponse> getRoutes(
            @Value("${server.servlet.context-path}") String contextPath, 
            FileStorageHandler handler
    ) {
        return route(POST(contextPath.concat("/upload")), handler::upload)
            .andRoute(GET(contextPath.concat("/list")), handler::list)
            .andRoute(GET(contextPath.concat("/download")), handler::download);
    }
}
