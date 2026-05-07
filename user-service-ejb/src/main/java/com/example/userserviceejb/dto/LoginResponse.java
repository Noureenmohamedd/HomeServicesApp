package com.example.userserviceejb.dto;

import com.example.userserviceejb.entity.UserRole;

public class LoginResponse {

    private String message;
    private Long userId;
    private UserRole role;
    private String token;

    public LoginResponse() {
    }

    public LoginResponse(String message, Long userId, UserRole role, String token) {
        this.message = message;
        this.userId = userId;
        this.role = role;
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
