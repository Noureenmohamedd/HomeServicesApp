package com.example.userserviceejb.dto;

import java.math.BigDecimal;

public class WalletRequest {

    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
