package com.example.kafka.event;

import com.example.kafka.config.TransactionProducerServiceConfig;
import com.example.kafka.mapper.TransactionMapper;
import com.example.kafka.messaging.TransactionEvent;
import com.example.kafka.model.Transaction;
import com.example.kafka.services.TransactionService;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class TransactionProducer {

    private static final Logger LOG = Logger.getLogger(TransactionProducer.class);

    private final TransactionMapper transactionMapper;
    private final TransactionService transactionService;
    private final TransactionProducerServiceConfig transactionProducerConfig;

    @Channel("transactions")
    Emitter<TransactionEvent> emitter;

    public TransactionProducer(
            TransactionMapper transactionMapper,
            TransactionService transactionService,
            TransactionProducerServiceConfig transactionProducerConfig
    ) {
        this.transactionMapper = transactionMapper;
        this.transactionService = transactionService;
        this.transactionProducerConfig = transactionProducerConfig;
    }

    public CompletionStage<Void> produceTransaction() {
        LOG.info("Generating transaction...");
        Transaction transaction = transactionService.generateTransaction();
        return emitter.send(transactionMapper.dtoToEvent(transaction));
    }

}
