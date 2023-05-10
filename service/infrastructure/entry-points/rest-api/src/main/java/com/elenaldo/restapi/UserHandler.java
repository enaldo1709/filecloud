package com.elenaldo.restapi;


import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.elenaldo.restapi.dto.UserDto;
import com.elenaldo.usecase.UserService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class UserHandler {
    private final UserService service;


    public Mono<ServerResponse> getUser(ServerRequest req){
        return Mono.justOrEmpty(req.queryParam("username"))
            .flatMap(service::getByUserName)
            .map(UserDto::mapFromUser)
            .flatMap(ServerResponse.ok()::bodyValue)
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        return ServerResponse.ok().body(service.list().map(UserDto::mapFromUser), UserDto.class);
    }


      
}
