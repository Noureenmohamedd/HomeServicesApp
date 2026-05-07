package org.example.notificationservice.wallet;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletEJB walletEJB;

    public WalletController(WalletEJB walletEJB) {
        this.walletEJB = walletEJB;
    }

    @PostMapping("/funds")
    public WalletResponse addFunds(@Valid @RequestBody WalletRequest request) {
        return new WalletResponse(request.customerId(), walletEJB.addFunds(request.customerId(), request.amount()));
    }

    @GetMapping("/{customerId}")
    public WalletResponse getBalance(@PathVariable Long customerId) {
        return new WalletResponse(customerId, walletEJB.getBalance(customerId));
    }
}
