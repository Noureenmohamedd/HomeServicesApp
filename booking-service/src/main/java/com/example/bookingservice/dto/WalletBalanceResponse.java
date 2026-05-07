package com.example.bookingservice.dto;

import java.math.BigDecimal;

public record WalletBalanceResponse(
        BigDecimal balance
) {
}
