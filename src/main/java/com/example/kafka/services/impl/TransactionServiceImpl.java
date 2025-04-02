package com.example.kafka.services.impl;

import com.example.kafka.enums.TransactionType;
import com.example.kafka.model.Transaction;
import com.example.kafka.services.TransactionService;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@ApplicationScoped
public class TransactionServiceImpl implements TransactionService {

    private final Random random = new Random();

    private static final String[] ACCOUNT_IDS = {
            "account-12345",
            "account-67890",
            "account-54321",
            "account-09876"
    };

    private static final String[] LOCATIONS = {
            "SI",
            "RS",
            "GB",
            "US",
            "IT",
            "AT",
            "DE",
            "HR",
            "HK",
            "SO"
    };

    @Override
    public Transaction generateTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setAccountId(getRandomAccountId());
        transaction.setTimestamp(Instant.now());
        transaction.setTransactionType(getRandomTransactionType());
        transaction.setAmount(getRandomAmount());
        transaction.setLocation(getRandomLocation());

        return transaction;
    }

    /**
     * Returns a random account id from the predefined list of account ids.
     */
    private String getRandomAccountId() {
        return ACCOUNT_IDS[random.nextInt(ACCOUNT_IDS.length)];
    }

    /**
     * Returns a random transaction type.
     */
    private TransactionType getRandomTransactionType() {
        return TransactionType.values()[random.nextInt(TransactionType.values().length)];
    }

    /**
     * Returns a random amount between 0.01 and 12000.00.
     */
    private BigDecimal getRandomAmount() {
        return BigDecimal.valueOf(0.01 + (12000 - 0.01) * random.nextDouble());
    }

    /**
     * Returns a random country code with a bias towards SI.
     *
     * @return a country code, where SI is returned 90% of the time and a random country code from the locations array 10% of the time.
     */
    private String getRandomLocation() {
        return random.nextDouble() < 0.9 ? "SI" : LOCATIONS[random.nextInt(LOCATIONS.length)];
    }
}
