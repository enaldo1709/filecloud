package com.elenaldo.postgresql;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import com.elenaldo.model.User;
import com.elenaldo.model.gateways.UserRepository;
import com.elenaldo.postgresql.util.UserMapper;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class UsersPostgreSQLAdapter implements UserRepository{
    private final DatabaseClient client;

    @Override
    public Mono<User> getByUserName(String username) {
        String query = "SELECT username, email, personal_name, user_password, date_created, public_key, private_key"+
            " FROM schfiles.users"+
            " WHERE username=:username";

        return client.sql(query)
            .bind("username", username)
            .fetch()
            .first()
            .flatMap(UserMapper::mapMapToDao)
            .flatMap(UserMapper::mapToUser);
    }

    @Override
    public Mono<User> getByEmail(String email) {
        String query = "SELECT username, email, personal_name, user_password, date_created, public_key, private_key "+
            "FROM schfiles.users"+
            "WHERE email = :email";

        return client.sql(query)
            .bind("email", email)
            .fetch()
            .first()
            .flatMap(UserMapper::mapMapToDao)
            .flatMap(UserMapper::mapToUser);
    }

    @Override
    public Mono<Boolean> existByUserName(String username) {
        String query = "SELECT EXISTS (SELECT username FROM schfiles.users WHERE username=:user)";

        return client.sql(query)
            .bind("user", username)
            .fetch()
            .one()
            .map(m -> m.get("exists"))
            .map(Boolean.class::cast);
    }

    @Override
    public Mono<Boolean> existByEmail(String email) {
        String query = "SELECT EXISTS (SELECT username FROM schfiles.users WHERE email = :email)";

        return client.sql(query)
            .bind("email", email)
            .fetch()
            .one()
            .map(m -> m.get("exists"))
            .map(Boolean.class::cast);
    }

    @Override
    public Mono<Boolean> existByUserNameAndEmail(String username, String email) {
        String query 
            = "SELECT EXISTS (SELECT username FROM schfiles.users WHERE username = :username AND email = :email)";

        return client.sql(query)
            .bind("username", username)
            .bind("email", email)
            .fetch()
            .one()
            .map(m -> m.get("exists"))
            .map(Boolean.class::cast);
    }

    @Override
    public Flux<User> list() {
        String query = "SELECT username, email, personal_name, user_password, date_created, public_key, private_key "+
            "FROM schfiles.users";

        return client.sql(query)
            .fetch()
            .all()
            .flatMap(UserMapper::mapMapToDao)
            .flatMap(UserMapper::mapToUser);
    }

    @Override
    public Mono<Long> create(User user) {
        String query = "INSERT INTO schfiles.users"
            + "(username, email, personal_name, user_password, date_created, public_key, private_key) "
            + "VALUES(:username, :email, :personal_name, :user_password, "
            +"to_timestamp(:date_created,'YYYY-MM-DDTHH24:MI:SS.MS'), :public_key, :private_key)";

        return UserMapper.mapUserToDao(user)
            .flatMap(
                dao -> client.sql(query)
                    .bind("username", dao.getUsername())
                    .bind("email", dao.getEmail())
                    .bind("personal_name", dao.getPersonalName())
                    .bind("user_password", dao.getUserPassword())
                    .bind("date_created", dao.getDateCreated())
                    .bind("public_key", dao.getPublicKey())
                    .bind("private_key", dao.getPrivateKey())
                    .fetch()
                    .rowsUpdated()
            );
    }

    @Override
    public Mono<Long> update(User user) {
        String query = "UPDATE schfiles.users"
            + "SET email = :email, personal_name = :personal_name, user_password = :user_password"
            + "WHERE username=:username";

        return UserMapper.mapUserToDao(user)
            .flatMap(
                dao -> client.sql(query)
                    .bind("username", dao.getUsername())
                    .bind("email", dao.getEmail())
                    .bind("personal_name", dao.getPersonalName())
                    .bind("user_password", dao.getUserPassword())
                    .fetch()
                    .rowsUpdated()
            );

    }

    @Override
    public Mono<Long> delete(String username) {
        String query = "DELETE FROM schfiles.users"
            +"WHERE username = :username";
        
        return client.sql(query)
            .bind("username", username)
            .fetch()
            .rowsUpdated();
    }
}
