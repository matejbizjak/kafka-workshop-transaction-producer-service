package com.example.kafka.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.time.Duration;

@ConfigMapping(prefix = "transaction-producer-service")
public interface TransactionProducerServiceConfig {

    @WithName("transaction-interval")
    Duration transactionInterval();
}
