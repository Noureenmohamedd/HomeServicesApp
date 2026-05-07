package com.example.bookingservice.dto;

import java.math.BigDecimal;

public record DeductWalletRequest(
        BigDecimal amount
) {
}
