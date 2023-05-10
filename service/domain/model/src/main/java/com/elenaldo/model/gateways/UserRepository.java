package com.elenaldo.model.gateways;

import com.elenaldo.model.User;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository {
    public Mono<User> getByUserName(String username);
    public Mono<User> getByEmail(String email);
    public Mono<Boolean> existByUserName(String username);
    public Mono<Boolean> existByEmail(String email);
    public Mono<Boolean> existByUserNameAndEmail(String username, String email);
    public Flux<User> list();
    public Mono<Long> create(User user);
    public Mono<Long> update(User user);
    public Mono<Long> delete(String username);
}
