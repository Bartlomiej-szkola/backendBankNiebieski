package org.example.backendbankuniebieskiego.controller;

import org.example.backendbankuniebieskiego.model.ClientAccount;
import org.example.backendbankuniebieskiego.service.BankService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

// nowe
import org.example.backendbankuniebieskiego.model.BankTransaction;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;


@RestController
@RequestMapping("/api/bank")
public class BankController {

    private final BankService bankService;
    private final RestTemplate restTemplate;
    private final String BLIK_URL;

    public BankController(BankService bankService, RestTemplate restTemplate, @Value("${blik.url}") String blikUrl) {
        this.bankService = bankService;
        this.restTemplate = restTemplate;
        this.BLIK_URL = blikUrl;
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

    @GetMapping("/account/by-phone/{phone}")
    public ResponseEntity<String> getAccountNumberByPhone(@PathVariable String phone) {
        // Prosimy serwis o znalezienie numeru konta
        return bankService.getAccountNumberByPhone(phone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

    // Endpoint dla płatności zbliżeniowych
    @PostMapping("/card/charge")
    public ResponseEntity<String> chargeCard(
            @RequestParam String cardUid,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "Płatność Kartą") String description) {

        boolean success = bankService.chargeByCardUid(cardUid, amount, description);

        if (success) {
            return ResponseEntity.ok("Płatność kartą zaakceptowana");
        } else {
            return ResponseEntity.badRequest().body("Odrzucono: Brak środków lub nieznana karta");
        }
    }

    // --- NOWE ENDPOINTY (Pośrednicy do BLIKa) ---

    // 1. Aplikacja prosi bank o kod BLIK
    @PostMapping("/blik/generate")
    public ResponseEntity<String> generateBlik(@RequestParam String accountNumber) {
        // Bank z automatu dopisuje swoje ID ("BLUE_BANK"), aplikacja nie musi tego wiedzieć
        String url = BLIK_URL + "/generate?accountNumber=" + accountNumber + "&bankId=BLUE_BANK";
        return restTemplate.postForEntity(url, null, String.class);
    }

    // 2. Aplikacja pyta bank o oczekujące płatności
    @GetMapping("/blik/pending/{accountNumber}")
    public ResponseEntity<String> checkPendingBlik(@PathVariable String accountNumber) {
        String url = BLIK_URL + "/pending/" + accountNumber;
        return restTemplate.getForEntity(url, String.class);
    }

    // 3. Aplikacja wysyła do banku zatwierdzenie płatności
    @PostMapping("/blik/authorize")
    public ResponseEntity<String> authorizeBlik(@RequestParam String accountNumber, @RequestParam boolean isApproved) {
        String url = BLIK_URL + "/authorize?accountNumber=" + accountNumber + "&isApproved=" + isApproved;
        return restTemplate.postForEntity(url, null, String.class);
    }

    // 4. Aplikacja zleca przelew na telefon (Bank przekazuje do BLIK-a)
    @PostMapping("/blik/transfer")
    public ResponseEntity<String> transferToPhone(
            @RequestParam String fromAccount,
            @RequestParam String toPhone,
            @RequestParam BigDecimal amount) {

        // Bank w locie przekazuje żądanie do BLIK
        String url = BLIK_URL + "/transfer?fromAccount=" + fromAccount + "&toPhone=" + toPhone + "&amount=" + amount;
        return restTemplate.postForEntity(url, null, String.class);
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> depositAccount(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "Przelew przychodzący") String description) {
        boolean success = bankService.depositAccount(accountNumber, amount, description);
        if (success) return ResponseEntity.ok("Wpłata udana");
        return ResponseEntity.badRequest().body("Nieznane konto");
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