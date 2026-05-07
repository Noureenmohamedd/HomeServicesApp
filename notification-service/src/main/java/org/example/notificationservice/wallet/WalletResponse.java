package org.example.notificationservice.wallet;

import java.math.BigDecimal;

public record WalletResponse(
        Long customerId,
        BigDecimal balance
) {
}
