package org.example.notificationservice.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WalletRequest(
        @NotNull Long customerId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
