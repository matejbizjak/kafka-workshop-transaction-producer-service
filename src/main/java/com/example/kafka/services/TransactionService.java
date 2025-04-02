package com.example.kafka.services;

import com.example.kafka.model.Transaction;

public interface TransactionService {

    /**
     * Generate a random transaction.
     */
    Transaction generateTransaction();
}
