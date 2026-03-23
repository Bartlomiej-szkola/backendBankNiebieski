package org.example.backendbankuniebieskiego.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_accounts")
public class ClientAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber; // np. 26 cyfr, albo proste ID dla testów

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String ownerSurname;

    @Column(nullable = false)
    private BigDecimal balance; // Stan konta

    @Column(nullable = false)
    private String cardUid; // Unikalny identyfikator sprzętowy karty/NFC

    @Column(unique = true) // To sprawi, że Hibernate utworzy kolumnę z kluczem UNIQUE
    private String phoneNumber;

    // Pole pomocnicze na przyszłość do logowania z aplikacji Android
    private String mobileAppPin;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerSurname() {
        return ownerSurname;
    }

    public void setOwnerSurname(String ownerSurname) {
        this.ownerSurname = ownerSurname;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getMobileAppPin() {
        return mobileAppPin;
    }

    public void setMobileAppPin(String mobileAppPin) {
        this.mobileAppPin = mobileAppPin;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCardUid() {
        return cardUid;
    }

    public void setCardUid(String cardUid) {
        this.cardUid = cardUid;
    }
}