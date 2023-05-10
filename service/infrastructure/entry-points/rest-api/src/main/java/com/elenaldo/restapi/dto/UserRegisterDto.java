package com.elenaldo.restapi.dto;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.elenaldo.model.User;

public record UserRegisterDto(String username, String name, String email, String password) {
    
    public User mapToCreate() {
        User user = User.builder()
            .username(username)
            .email(email)
            .name(name)
            .created(LocalDateTime.now())
            .build();
        try {
            user.setKeys(KeyPairGenerator.getInstance("RSA").generateKeyPair());
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, user.getKeys().getPublic());

            user.setPassword(new String(cipher.doFinal(password.substring(0, 189).getBytes(UTF_8)),UTF_8));

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            
        }

        return user;
    }

}
