package com.elenaldo.usecase;

import com.elenaldo.model.User;
import com.elenaldo.model.gateways.UserRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;


    public Mono<User> getByUserName(String username) {
        return repository.existByUserName(username)
            .flatMap(b -> b.booleanValue() ? repository.getByUserName(username) : Mono.empty());
    }

    public Flux<User> list() {
        return repository.list();
    }
}
