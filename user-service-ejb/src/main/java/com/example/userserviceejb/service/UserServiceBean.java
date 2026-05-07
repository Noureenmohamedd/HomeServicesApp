package com.example.userserviceejb.service;

import com.example.userserviceejb.dto.RegisterAdminRequest;
import com.example.userserviceejb.dto.RegisterCustomerRequest;
import com.example.userserviceejb.dto.RegisterProviderRequest;
import com.example.userserviceejb.entity.TransactionRecord;
import com.example.userserviceejb.entity.User;
import com.example.userserviceejb.entity.UserRole;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Stateless
public class UserServiceBean {

    @PersistenceContext(unitName = "userPU")
    private EntityManager entityManager;

    public User registerAdmin(RegisterAdminRequest request) {
        validateCredentials(request.getUsername(), request.getPassword());
        ensureUsernameIsUnique(request.getUsername());

        User admin = new User(
                request.getUsername().trim(),
                request.getPassword(),
                BigDecimal.ZERO,
                UserRole.ADMIN,
                null
        );
        entityManager.persist(admin);
        return admin;
    }

    public User registerCustomer(RegisterCustomerRequest request) {
        validateCredentials(request.getUsername(), request.getPassword());

        BigDecimal balance = request.getBalance() == null ? BigDecimal.ZERO : request.getBalance();
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }

        ensureUsernameIsUnique(request.getUsername());

        User user = new User(
                request.getUsername().trim(),
                request.getPassword(),
                balance,
                UserRole.CUSTOMER,
                null
        );
        entityManager.persist(user);
        return user;
    }

    public User registerProvider(RegisterProviderRequest request) {
        validateCredentials(request.getUsername(), request.getPassword());
        if (request.getProfessionType() == null) {
            throw new IllegalArgumentException("Profession type is required");
        }

        ensureUsernameIsUnique(request.getUsername());

        User user = new User(
                request.getUsername().trim(),
                request.getPassword(),
                BigDecimal.ZERO,
                UserRole.PROVIDER,
                request.getProfessionType()
        );
        entityManager.persist(user);
        return user;
    }

    public User addUser(User user) {
        validateCredentials(user.getUsername(), user.getPassword());

        if (user.getBalance() == null) {
            user.setBalance(BigDecimal.ZERO);
        }
        if (user.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        if (user.getRole() == null) {
            user.setRole(UserRole.CUSTOMER);
        }
        if (user.getRole() != UserRole.PROVIDER) {
            user.setProfessionType(null);
        }

        ensureUsernameIsUnique(user.getUsername());
        user.setUsername(user.getUsername().trim());
        entityManager.persist(user);
        return user;
    }

    public List<User> getAllUsers() {
        return entityManager
                .createQuery("SELECT u FROM User u ORDER BY u.id", User.class)
                .getResultList();
    }

    public List<User> getRegisteredUsers() {
        return entityManager
                .createQuery(
                        "SELECT u FROM User u WHERE u.role IN (:customerRole, :providerRole) ORDER BY u.id",
                        User.class
                )
                .setParameter("customerRole", UserRole.CUSTOMER)
                .setParameter("providerRole", UserRole.PROVIDER)
                .getResultList();
    }

    public List<User> getAdminUsers() {
        return entityManager
                .createQuery("SELECT u FROM User u WHERE u.role = :adminRole ORDER BY u.id", User.class)
                .setParameter("adminRole", UserRole.ADMIN)
                .getResultList();
    }

    public Optional<User> getUserById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(User.class, id));
    }

    public Optional<User> findByUsername(String username) {
        if (isBlank(username)) {
            return Optional.empty();
        }

        List<User> users = entityManager
                .createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username.trim())
                .setMaxResults(1)
                .getResultList();

        return users.stream().findFirst();
    }

    public Optional<BigDecimal> getWalletBalance(Long id) {
        return getUserById(id).map(User::getBalance);
    }

    public Optional<User> addFunds(Long id, BigDecimal amount) {
        validatePositiveAmount(amount);

        Optional<User> userOptional = getUserById(id);
        userOptional.ifPresent(user -> {
            BigDecimal updatedBalance = user.getBalance().add(amount);
            user.setBalance(updatedBalance);
            recordTransaction(
                    user,
                    "WALLET_TOP_UP",
                    amount,
                    updatedBalance,
                    "Funds added to wallet"
            );
        });
        return userOptional;
    }

    public double deductWallet(Long userId, double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        User user = getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        BigDecimal deductionAmount = BigDecimal.valueOf(amount);
        if (user.getBalance().compareTo(deductionAmount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        BigDecimal updatedBalance = user.getBalance().subtract(deductionAmount);
        user.setBalance(updatedBalance);
        recordTransaction(
                user,
                "BOOKING_DEDUCTION",
                deductionAmount,
                updatedBalance,
                "Wallet deducted during booking creation"
        );
        return updatedBalance.doubleValue();
    }

    public List<TransactionRecord> getAllTransactionRecords() {
        return entityManager
                .createQuery("SELECT t FROM TransactionRecord t ORDER BY t.createdAt DESC, t.id DESC", TransactionRecord.class)
                .getResultList();
    }

    private void recordTransaction(User user, String transactionType, BigDecimal amount,
                                   BigDecimal balanceAfter, String description) {
        entityManager.persist(new TransactionRecord(
                user.getId(),
                user.getUsername(),
                transactionType,
                amount,
                balanceAfter,
                description
        ));
    }

    private void ensureUsernameIsUnique(String username) {
        if (findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
    }

    private void validateCredentials(String username, String password) {
        if (isBlank(username)) {
            throw new IllegalArgumentException("Username is required");
        }
        if (isBlank(password)) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class UserNotFoundException extends RuntimeException {

        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
