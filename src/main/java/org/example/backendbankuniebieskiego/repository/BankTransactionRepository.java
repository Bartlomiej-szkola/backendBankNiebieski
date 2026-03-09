package org.example.backendbankuniebieskiego.repository;

import org.example.backendbankuniebieskiego.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    // Pobiera historię konta posortowaną od najnowszych
    List<BankTransaction> findByAccountNumberOrderByTimestampDesc(String accountNumber);
}