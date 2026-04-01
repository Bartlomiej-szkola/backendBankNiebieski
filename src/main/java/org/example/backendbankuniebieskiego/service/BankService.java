package org.example.backendbankuniebieskiego.service;

import org.example.backendbankuniebieskiego.model.ClientAccount;
import org.example.backendbankuniebieskiego.model.PaymentCard;
import org.example.backendbankuniebieskiego.repository.ClientAccountRepository;
import org.example.backendbankuniebieskiego.repository.PaymentCardRepository;
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
    private final PaymentCardRepository paymentCardRepository;

    public BankService(ClientAccountRepository accountRepository, BankTransactionRepository transactionRepository, PaymentCardRepository paymentCardRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.paymentCardRepository = paymentCardRepository;
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
        // 1. Szukamy aktywnej karty po UID
        Optional<PaymentCard> cardOpt = paymentCardRepository.findByCardUidAndIsActiveTrue(cardUid);

        if (cardOpt.isPresent()) {
            PaymentCard card = cardOpt.get();
            // 2. Pobieramy konto przypisane do tej karty
            ClientAccount account = card.getClientAccount();

            // 3. Sprawdzamy, czy na koncie są środki
            if (account.getBalance().compareTo(amount) >= 0) {

                // Zdejmujemy środki
                account.setBalance(account.getBalance().subtract(amount));
                accountRepository.save(account);

                // Zapisujemy historię transakcji
                BankTransaction transaction = new BankTransaction();
                transaction.setAccountNumber(account.getAccountNumber());
                transaction.setAmount(amount.negate()); // Kwota ujemna
                transaction.setType("KARTA");
                transaction.setDescription(description);
                transaction.setTimestamp(LocalDateTime.now());
                transactionRepository.save(transaction);

                return true;
            }
        }
        return false; // Brak aktywnej karty lub brak środków na koncie docelowym
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

    // --- WYDAWANIE NOWEJ KARTY ---
    public PaymentCard addCardToAccount(String accountNumber, String cardUid) {
        // Szukamy konta
        ClientAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Konto nie istnieje!"));

        // Tworzymy nową kartę przypisaną do tego konta
        PaymentCard card = new PaymentCard();
        card.setCardUid(cardUid);
        card.setClientAccount(account);
        card.setActive(true);

        return paymentCardRepository.save(card);
    }
}