package com.example.userserviceejb.dto;

import com.example.userserviceejb.entity.ProfessionType;

public class RegisterProviderRequest {

    private String username;
    private String password;
    private ProfessionType professionType;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ProfessionType getProfessionType() {
        return professionType;
    }

    public void setProfessionType(ProfessionType professionType) {
        this.professionType = professionType;
    }
}
