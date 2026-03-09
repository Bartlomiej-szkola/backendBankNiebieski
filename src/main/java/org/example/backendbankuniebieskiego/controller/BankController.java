package org.example.backendbankuniebieskiego.controller;

import org.example.backendbankuniebieskiego.model.ClientAccount;
import org.example.backendbankuniebieskiego.service.BankService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

// nowe
import org.example.backendbankuniebieskiego.model.BankTransaction;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;


@RestController
@RequestMapping("/api/bank")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    // ==========================================
    // ENDPOINTY DLA APLIKACJI MOBILNEJ (ANDROID)
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        boolean isValid = bankService.verifyLogin(loginRequest.getAccountNumber(), loginRequest.getPin());
        if (isValid) {
            return ResponseEntity.ok("Zalogowano pomyślnie");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Błędny numer konta lub PIN");
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ClientAccount> getAccount(@PathVariable String accountNumber) {
        return bankService.getAccountInfo(accountNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/transactions/{accountNumber}")
    public ResponseEntity<List<BankTransaction>> getHistory(@PathVariable String accountNumber) {
        return ResponseEntity.ok(bankService.getAccountHistory(accountNumber));
    }


    // ==========================================
    // ENDPOINTY DLA SYSTEMU BLIK / WEWNĘTRZNE
    // ==========================================

    @PostMapping("/account")
    public ClientAccount createAccount(@RequestBody ClientAccount account) {
        return bankService.createAccount(account);
    }

    // Zmodyfikowany endpoint - teraz przyjmuje też "description" (nazwę sklepu)
    @PostMapping("/charge")
    public ResponseEntity<String> chargeAccount(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "Płatność BLIK") String description) {

        boolean success = bankService.chargeAccount(accountNumber, amount, description);

        if (success) {
            return ResponseEntity.ok("Transakcja zaakceptowana");
        } else {
            return ResponseEntity.badRequest().body("Odrzucono: Brak środków lub nieznane konto");
        }
    }
}

// Klasa pomocnicza do logowania (możesz ją wydzielić do osobnego pliku LoginRequest.java)
class LoginRequest {
    private String accountNumber;
    private String pin;

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}