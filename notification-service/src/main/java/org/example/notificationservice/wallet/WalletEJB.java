package org.example.notificationservice.wallet;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class WalletEJB {

    private final ConcurrentMap<Long, BigDecimal> balances = new ConcurrentHashMap<>();

    public boolean hasSufficientBalance(Long customerId, BigDecimal amount) {
        return getBalance(customerId).compareTo(amount) >= 0;
    }

    public BigDecimal addFunds(Long customerId, BigDecimal amount) {
        return balances.merge(customerId, amount, BigDecimal::add);
    }

    public BigDecimal deduct(Long customerId, BigDecimal amount) {
        return balances.compute(customerId, (id, balance) -> {
            BigDecimal currentBalance = balance == null ? BigDecimal.ZERO : balance;
            if (currentBalance.compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient wallet balance");
            }
            return currentBalance.subtract(amount);
        });
    }

    public BigDecimal refund(Long customerId, BigDecimal amount) {
        return balances.merge(customerId, amount, BigDecimal::add);
    }

    public BigDecimal getBalance(Long customerId) {
        return balances.getOrDefault(customerId, BigDecimal.ZERO);
    }
}
