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

    public Optional<String> getAccountNumberByPhone(String phone) {
        // Zakładam, że w BankService masz wstrzyknięte ClientAccountRepository jako 'repository' lub 'accountRepository'
        return accountRepository.findByPhoneNumber(phone)
                .map(ClientAccount::getAccountNumber);
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

    @Transactional
    public boolean chargeByCardUid(String cardUid, BigDecimal amount, String description) {
        Optional<ClientAccount> accountOpt = accountRepository.findByCardUid(cardUid);

        if (accountOpt.isPresent()) {
            ClientAccount account = accountOpt.get();
            if (account.getBalance().compareTo(amount) >= 0) {
                // Zdejmujemy środki
                account.setBalance(account.getBalance().subtract(amount));
                accountRepository.save(account);

                // Zapisujemy historię
                BankTransaction transaction = new BankTransaction();
                transaction.setAccountNumber(account.getAccountNumber());
                transaction.setAmount(amount.negate());
                transaction.setType("KARTA");
                transaction.setDescription(description);
                transaction.setTimestamp(LocalDateTime.now());
                transactionRepository.save(transaction);

                return true;
            }
        }
        return false; // Brak karty w bazie lub brak środków
    }

    @Transactional
    public boolean depositAccount(String accountNumber, BigDecimal amount, String description) {
        Optional<ClientAccount> accountOpt = accountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isPresent()) {
            ClientAccount account = accountOpt.get();
            // Dodajemy środki
            account.setBalance(account.getBalance().add(amount));
            accountRepository.save(account);

            // Zapis do historii (kwota na plusie)
            BankTransaction transaction = new BankTransaction();
            transaction.setAccountNumber(accountNumber);
            transaction.setAmount(amount); // Dodatnia!
            transaction.setType("PRZELEW NA TELEFON");
            transaction.setDescription(description);
            transaction.setTimestamp(LocalDateTime.now());
            transactionRepository.save(transaction);
            return true;
        }
        return false;
    }
}