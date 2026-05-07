package com.example.userserviceejb.dto;

import com.example.userserviceejb.entity.ProfessionType;
import com.example.userserviceejb.entity.UserRole;

public class TokenValidationResponse {

    private String message;
    private Long userId;
    private String username;
    private UserRole role;
    private ProfessionType professionType;

    public TokenValidationResponse() {
    }

    public TokenValidationResponse(String message, Long userId, String username, UserRole role, ProfessionType professionType) {
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.professionType = professionType;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public ProfessionType getProfessionType() {
        return professionType;
    }

    public void setProfessionType(ProfessionType professionType) {
        this.professionType = professionType;
    }
}
