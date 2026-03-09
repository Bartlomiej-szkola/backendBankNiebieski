package org.example.backendbankuniebieskiego.controller;

import org.example.backendbankuniebieskiego.model.ClientAccount;
import org.example.backendbankuniebieskiego.service.BankService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/bank")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    // 1. DLA APLIKACJI ANDROID: Pobranie stanu konta
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ClientAccount> getAccount(@PathVariable String accountNumber) {
        return bankService.getAccountInfo(accountNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 2. TWORZENIE KONTA (Do celów testowych, żeby mieć na czym pracować)
    @PostMapping("/account")
    public ClientAccount createAccount(@RequestBody ClientAccount account) {
        return bankService.createAccount(account);
    }

    // 3. DLA SERWERA BLIK: Obciążenie konta (Zapłata za zakupy)
    // Przykład: POST /api/bank/charge?accountNumber=12345&amount=150.50
    @PostMapping("/charge")
    public ResponseEntity<String> chargeAccount(@RequestParam String accountNumber, @RequestParam BigDecimal amount) {
        boolean success = bankService.chargeAccount(accountNumber, amount);

        if (success) {
            return ResponseEntity.ok("Transakcja zaakceptowana");
        } else {
            return ResponseEntity.badRequest().body("Odrzucono: Brak środków lub nieznane konto");
        }
    }
}