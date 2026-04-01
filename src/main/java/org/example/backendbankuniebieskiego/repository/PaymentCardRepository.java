package org.example.backendbankuniebieskiego.repository;

import org.example.backendbankuniebieskiego.model.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {

    // Szuka karty po numerze NFC, ale TYLKO takiej, która nie jest zablokowana (isActive = true)
    Optional<PaymentCard> findByCardUidAndIsActiveTrue(String cardUid);
}