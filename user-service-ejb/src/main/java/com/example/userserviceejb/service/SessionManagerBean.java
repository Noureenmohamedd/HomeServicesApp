package com.example.userserviceejb.service;

import com.example.userserviceejb.entity.User;
import com.example.userserviceejb.entity.UserRole;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class SessionManagerBean {

    private final Map<Long, UserRole> activeSessions = new ConcurrentHashMap<>();

    @Lock(LockType.WRITE)
    public void createSession(User user) {
        activeSessions.put(user.getId(), user.getRole());
    }

    @Lock(LockType.READ)
    public Optional<UserRole> getLoggedInUserRole(Long userId) {
        return Optional.ofNullable(activeSessions.get(userId));
    }

    @Lock(LockType.WRITE)
    public void removeSession(Long userId) {
        activeSessions.remove(userId);
    }
}
