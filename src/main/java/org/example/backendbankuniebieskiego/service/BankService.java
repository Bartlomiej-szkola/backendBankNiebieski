package org.example.backendbankuniebieskiego.service;

import org.example.backendbankuniebieskiego.model.ClientAccount;
import org.example.backendbankuniebieskiego.repository.ClientAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class BankService {

    private final ClientAccountRepository repository;

    public BankService(ClientAccountRepository repository) {
        this.repository = repository;
    }

    public ClientAccount createAccount(ClientAccount account) {
        return repository.save(account);
    }

    public Optional<ClientAccount> getAccountInfo(String accountNumber) {
        return repository.findByAccountNumber(accountNumber);
    }

    // Metoda, którą w przyszłości wywoła system BLIK, aby pobrać pieniądze!
    @Transactional
    public boolean chargeAccount(String accountNumber, BigDecimal amount) {
        Optional<ClientAccount> accountOpt = repository.findByAccountNumber(accountNumber);

        if (accountOpt.isPresent()) {
            ClientAccount account = accountOpt.get();

            // Sprawdzamy czy klient ma wystarczające środki
            if (account.getBalance().compareTo(amount) >= 0) {
                account.setBalance(account.getBalance().subtract(amount));
                repository.save(account);
                return true; // Transakcja udana
            }
        }
        return false; // Brak konta lub brak środków
    }
}