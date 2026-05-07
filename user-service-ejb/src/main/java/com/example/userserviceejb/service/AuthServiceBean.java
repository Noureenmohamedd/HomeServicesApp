package com.example.userserviceejb.service;

import com.example.userserviceejb.dto.LoginRequest;
import com.example.userserviceejb.dto.LoginResponse;
import com.example.userserviceejb.entity.User;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.Optional;

@Stateless
public class AuthServiceBean {

    @Inject
    private UserServiceBean userServiceBean;

    @Inject
    private SessionManagerBean sessionManagerBean;

    @Inject
    private JwtServiceBean jwtServiceBean;

    public Optional<LoginResponse> login(LoginRequest request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            return Optional.empty();
        }

        Optional<User> userOptional = userServiceBean.findByUsername(request.getUsername());
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();
        if (!user.getPassword().equals(request.getPassword())) {
            return Optional.empty();
        }

        sessionManagerBean.createSession(user);
        String token = jwtServiceBean.generateToken(user);
        return Optional.of(new LoginResponse("Login successful", user.getId(), user.getRole(), token));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
