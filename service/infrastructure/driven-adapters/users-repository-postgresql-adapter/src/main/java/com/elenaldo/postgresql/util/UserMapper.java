package com.elenaldo.postgresql.util;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

import com.elenaldo.model.User;
import com.elenaldo.postgresql.dao.UserDao;

import reactor.core.publisher.Mono;

public class UserMapper {
    private UserMapper() {}

    public static Mono<User> mapToUser(UserDao user) {
        return decodeKeys(user)
            .map(kp -> User.builder()
                .username(user.getUsername())
                .name(user.getPersonalName())
                .email(user.getEmail())
                .created(user.getDateCreated())
                .password(user.getUserPassword())
                .keys(kp)
                .build()
            );
    }

    public static Mono<UserDao> mapUserToDao(User user) {
        return Mono.just(
            UserDao.builder()
                .username(user.getUsername())
                .personalName(user.getName())
                .email(user.getEmail())
                .dateCreated(user.getCreated())
                .userPassword(user.getPassword())
                .publicKey(Base64.getEncoder().encodeToString(user.getKeys().getPublic().getEncoded()))
                .privateKey(Base64.getEncoder().encodeToString(user.getKeys().getPrivate().getEncoded()))
                .build()
        );
    }

    public static Mono<UserDao> mapMapToDao(Map<String,Object> map) {
        return Mono.just(
            UserDao.builder()
                .username((String)map.get("username"))
                .email((String)map.get("email"))
                .personalName((String)map.get("personal_name"))
                .userPassword((String)map.get("user_password"))
                .dateCreated((LocalDateTime)map.get("date_created"))
                .publicKey((String)map.get("public_key"))
                .privateKey((String)map.get("private_key"))
                .build()
        );
    }

    protected static Mono<KeyPair> decodeKeys(UserDao user) {
        try {
            String secretPemString = user.getPrivateKey();
		
            secretPemString = secretPemString.replace("-----BEGIN PRIVATE KEY-----", "");
            secretPemString = secretPemString.replace("-----END PRIVATE KEY-----", "");
            
            byte[] secretDecoded = Base64.getDecoder().decode(secretPemString);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(secretDecoded);
		    PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            String publicPemString = user.getPublicKey();

            publicPemString = publicPemString.replace("-----BEGIN PUBLIC KEY-----", "");
            publicPemString = publicPemString.replace("-----END PUBLIC KEY-----", "");
            
            byte[] publicDecoded = Base64.getDecoder().decode(publicPemString);
            keySpec = new X509EncodedKeySpec(publicDecoded);
		    PublicKey publicKey = keyFactory.generatePublic(keySpec);

            return Mono.just(new KeyPair(publicKey, privateKey));

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            return Mono.error(e);
        }
    }
}
