package org.example.backendbankuniebieskiego.service;

import org.example.backendbankuniebieskiego.model.ClientAccount;
import org.example.backendbankuniebieskiego.repository.ClientAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.backendbankuniebieskiego.model.BankTransaction;
import org.example.backendbankuniebieskiego.repository.BankTransactionRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BankService {

    private final ClientAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;

    public BankService(ClientAccountRepository accountRepository, BankTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public ClientAccount createAccount(ClientAccount account) {
        return accountRepository.save(account);
    }

    public Optional<ClientAccount> getAccountInfo(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    // --- NOWE: LOGOWANIE DLA APLIKACJI MOBILNEJ ---
    public boolean verifyLogin(String accountNumber, String pin) {
        Optional<ClientAccount> account = accountRepository.findByAccountNumber(accountNumber);
        // W prawdziwym banku PIN byłby hashowany (np. BCrypt), tu sprawdzamy plain-text
        return account.isPresent() && account.get().getMobileAppPin().equals(pin);
    }

    // --- NOWE: POBIERANIE HISTORII ---
    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findByAccountNumberOrderByTimestampDesc(accountNumber);
    }

    // --- ZAKTUALIZOWANE: OBCIĄŻENIE KONTA Z ZAPISEM DO HISTORII ---
    @Transactional
    public boolean chargeAccount(String accountNumber, BigDecimal amount, String description) {
        Optional<ClientAccount> accountOpt = accountRepository.findByAccountNumber(accountNumber);

        if (accountOpt.isPresent()) {
            ClientAccount account = accountOpt.get();

            if (account.getBalance().compareTo(amount) >= 0) {
                // 1. Zdejmujemy środki
                account.setBalance(account.getBalance().subtract(amount));
                accountRepository.save(account);

                // 2. Tworzymy wpis w historii
                BankTransaction transaction = new BankTransaction();
                transaction.setAccountNumber(accountNumber);
                transaction.setAmount(amount.negate()); // Ujemna kwota, bo to wydatek
                transaction.setType("BLIK");
                transaction.setDescription(description); // Np. "Płatność w: Sklep WPF"
                transaction.setTimestamp(LocalDateTime.now());

                transactionRepository.save(transaction);

                return true;
            }
        }
        return false;
    }
}