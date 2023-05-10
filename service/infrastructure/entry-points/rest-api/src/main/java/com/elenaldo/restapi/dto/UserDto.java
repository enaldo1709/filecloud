package com.elenaldo.restapi.dto;

import java.time.LocalDateTime;

import com.elenaldo.model.User;


public record UserDto(String username, String name, LocalDateTime created) {
    public static UserDto mapFromUser(User user) {
        return new UserDto(user.getUsername(), user.getName(), user.getCreated());
    }
} 
