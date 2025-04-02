package com.example.kafka.event;

import com.example.kafka.config.TransactionProducerServiceConfig;
import com.example.kafka.mapper.TransactionMapper;
import com.example.kafka.messaging.TransactionEvent;
import com.example.kafka.model.Transaction;
import com.example.kafka.services.TransactionService;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TransactionProducer {

    private static final Logger LOG = Logger.getLogger(TransactionProducer.class);

    private final TransactionMapper transactionMapper;
    private final TransactionService transactionService;
    private final TransactionProducerServiceConfig transactionProducerConfig;

    public TransactionProducer(
            TransactionMapper transactionMapper,
            TransactionService transactionService,
            TransactionProducerServiceConfig transactionProducerConfig
    ) {
        this.transactionMapper = transactionMapper;
        this.transactionService = transactionService;
        this.transactionProducerConfig = transactionProducerConfig;
    }

//    @Channel("transactions")
//    Emitter<TransactionEvent> emitter;
//
//    /**
//     * Produces a single transaction event.
//     * @return a CompletionStage that completes when the message is acknowledged by the Kafka client.
//     */
//    public CompletionStage<Void> produceTransaction() {
//        LOG.info("Generating transaction...");
//        Transaction transaction = transactionService.generateTransaction();
//        return emitter.send(transactionMapper.dtoToEvent(transaction));
//    }

    /**
     * Periodically produces a transaction event.
     * <p>
     * This method is automatically invoked by the Reactive Messaging engine at application startup,
     * thanks to the combination of {@code @Outgoing} and a {@link Multi} return type, which sets up
     * a continuous, asynchronous stream of events.
     * <p>
     * A key-value record is sent to the Kafka topic with the account ID as the key.
     * This ensures event ordering for the same account when multiple partitions are used.
     */
    @Outgoing("transactions")
    public Multi<Record<String, TransactionEvent>> produceTransactions() {
        return Multi.createFrom().ticks().every(transactionProducerConfig.transactionInterval())
                .map(unused -> {
                    LOG.info("Generating transaction...");

                    Transaction transaction = transactionService.generateTransaction();
                    return Record.of(transaction.getAccountId(), transactionMapper.dtoToEvent(transaction));
                });
    }
}
