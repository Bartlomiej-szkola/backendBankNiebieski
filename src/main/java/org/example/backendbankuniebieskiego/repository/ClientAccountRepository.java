package org.example.backendbankuniebieskiego.repository;

import org.example.backendbankuniebieskiego.model.ClientAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientAccountRepository extends JpaRepository<ClientAccount, Long> {
    Optional<ClientAccount> findByPhoneNumber(String phoneNumber);
    Optional<ClientAccount> findByAccountNumber(String accountNumber);
    Optional<ClientAccount> findByCardUid(String cardUid);
}