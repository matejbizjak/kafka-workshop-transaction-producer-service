# Workflow

Clone the repositories:

- https://github.com/matejbizjak/kafka-workshop-transaction-producer-service
- https://github.com/matejbizjak/kafka-workshop-fraud-detection-service
- https://github.com/matejbizjak/kafka-workshop-transaction-dashboard-service

Switch to the branch `starting-point`

Start Docker Compose: `docker compose -p kafka up -d` in the `transaction-producer-service`

Create the topic using the Kafdrop tool: `transactions` with 3 partitions

## transaction-producer:

v TransactionProducer:

```java

@Outgoing("transactions")
public Multi<Record<String, TransactionEvent>> produceTransactions() {
    return Multi.createFrom().ticks().every(transactionProducerConfig.transactionInterval())
            .map(unused -> {
                LOG.info("Generating transaction...");

                Transaction transaction = transactionService.generateTransaction();
                return Record.of(transaction.getAccountId(), transactionMapper.dtoToEvent(transaction));
            });
}
```

Use this import: `import io.smallrye.reactive.messaging.kafka.Record;`

And comment out or delete the following lines:
```java
//    @Channel("transactions")
//    Emitter<TransactionEvent> emitter;

//    /**
//     * Produces a single transaction event.
//     * @return a CompletionStage that completes when the message is acknowledged by the Kafka client.
//     */
//    public CompletionStage<Void> produceTransaction() {
//        LOG.info("Generating transaction...");
//        Transaction transaction = transactionService.generateTransaction();
//        return emitter.send(transactionMapper.dtoToEvent(transaction));
//    }
```

## fraud-detection-service

In `TransactionProcessor`:

```java

@Channel("fraudulent-transactions")
MutinyEmitter<TransactionEvent> fraudulentTransactionsEmitter;

@Channel("verified-transactions")
MutinyEmitter<TransactionEvent> verifiedTransactionsEmitter;

@Incoming("transactions-fraud-detection")
public Uni<Void> detectFraudReactive(TransactionEvent transactionEvent) {
    Transaction transaction = transactionMapper.eventToDto(transactionEvent);

    if (fraudDetectionService.isSuspicious(transaction)) {
        LOG.warn("Suspicious transaction: " + transaction.toString());
        return fraudulentTransactionsEmitter.send(transactionMapper.dtoToEvent(transaction));
    } else {
        return verifiedTransactionsEmitter.send(transactionMapper.dtoToEvent(transaction));
    }
}
```

## transaction-producer:

Add to config:

```properties
mp.messaging.connector.smallrye-kafka.apicurio.registry.url=http://localhost:8081/apis/registry/v2
# disable automatic detection of the serializers
quarkus.messaging.kafka.serializer-autodetection.enabled=false
mp.messaging.outgoing.transactions.value.serializer=com.example.kafka.serdes.CustomJsonSchemaKafkaSerializer
mp.messaging.outgoing.transactions.apicurio.registry.artifact.schema.location=schemas/transaction.json
mp.messaging.outgoing.transactions.apicurio.registry.auto-register=true
```

## fraud-detection-service

Add to config:

```properties
mp.messaging.connector.smallrye-kafka.apicurio.registry.url=http://localhost:8081/apis/registry/v2
# disable automatic detection of the serializers
quarkus.messaging.kafka.serializer-autodetection.enabled=false
mp.messaging.incoming.transactions-log.value.deserializer=com.example.kafka.serdes.CustomJsonSchemaKafkaDeserializer
mp.messaging.incoming.transactions-log.apicurio.registry.artifact.schema.location=schemas/transaction.json
mp.messaging.incoming.transactions-log.auto-register=false
mp.messaging.incoming.transactions-log.failure-strategy=ignore
mp.messaging.incoming.transactions-log.fail-on-deserialization-failure=false
mp.messaging.incoming.transactions-fraud-detection.value.deserializer=com.example.kafka.serdes.CustomJsonSchemaKafkaDeserializer
mp.messaging.incoming.transactions-fraud-detection.apicurio.registry.artifact.schema.location=schemas/transaction.json
mp.messaging.incoming.transactions-fraud-detection.auto-register=false
mp.messaging.incoming.transactions-fraud-detection.failure-strategy=ignore
mp.messaging.incoming.transactions-fraud-detection.fail-on-deserialization-failure=false
mp.messaging.outgoing.verified-transactions.value.serializer=com.example.kafka.serdes.CustomJsonSchemaKafkaSerializer
mp.messaging.outgoing.verified-transactions.apicurio.registry.artifact.schema.location=schemas/transaction.json
mp.messaging.outgoing.verified-transactions.apicurio.registry.auto-register=true
mp.messaging.outgoing.fraudulent-transactions.value.serializer=com.example.kafka.serdes.CustomJsonSchemaKafkaSerializer
mp.messaging.outgoing.fraudulent-transactions.apicurio.registry.artifact.schema.location=schemas/transaction.json
mp.messaging.outgoing.fraudulent-transactions.apicurio.registry.auto-register=true
```

## transaction-producer:

Add to `transaction.json`:

```json
"category": {
"type": "string",
"maxLength": 256,
"description": "Category of transaction."
}
```

Add to `Transaction`:

```java
private String category;

// and getters/setters
```

Add to `TransactionServiceImpl`:

```java
transaction.setCategory("ENERGY");
```

Set the schema to forward compatibility and show the new schema in the UI

## transaction-dashboard-service

Add to `DashboardAggregator`:

```java

@Produces
public Topology buildDailyAccountBalanceTopology() {
    StreamsBuilder builder = new StreamsBuilder();
    builder.stream(VERIFIED_TRANSACTIONS_TOPIC, Consumed.with(Serdes.String(), transactionEventSerde))
            .map((key, transactionEvent) -> new KeyValue<>(key, transactionMapper.eventToDto(transactionEvent)))
            .map((key, transaction) -> {
                LocalDate date = transaction.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
                String newKey = date + ":" + transaction.getAccountId();
                return KeyValue.pair(newKey, transaction);
            })
            .groupByKey(Grouped.with(Serdes.String(), transactionSerde))
            .aggregate(() -> null,
                    (key, txn, agg) -> {
                        if (agg == null) {
                            return new AccountBalanceDailyAggregation(txn);
                        } else {
                            agg.applyTransaction(txn);
                            return agg;
                        }
                    },
                    Materialized.<String, AccountBalanceDailyAggregation, KeyValueStore<Bytes, byte[]>>as(ACCOUNT_DAILY_BALANCES_STORE)
                            .withKeySerde(Serdes.String())
                            .withValueSerde(aggregationSerde)
            )
            .toStream()
            .mapValues((readOnlyKey, value) -> accountBalanceDailyAggregationMapper.dtoToEvent(value))
            .to(DAILY_BALANCE_TOPIC, Produced.with(Serdes.String(), aggregationEventSerde));

    return builder.build();
}
```

Implement `AccountBalanceServiceImpl`:

```java

@ApplicationScoped
public class AccountBalanceServiceImpl implements AccountBalanceService {

    private final KafkaStreams streams;

    public AccountBalanceServiceImpl(KafkaStreams streams) {
        this.streams = streams;
    }

    @Override
    public List<AccountBalanceDailyAggregation> getBalancesByDate(LocalDate date) {
        ReadOnlyKeyValueStore<String, AccountBalanceDailyAggregation> store = streams.store(StoreQueryParameters
                .fromNameAndType(DashboardAggregator.ACCOUNT_DAILY_BALANCES_STORE, QueryableStoreTypes.keyValueStore()));

        List<AccountBalanceDailyAggregation> result = new ArrayList<>();

        // keys are stored in format [date]:[accountId] so we can do a prefix scan by date
        try (var iterator = store.prefixScan(date.toString(), new StringSerializer())) {
            iterator.forEachRemaining(entry -> {
                if (entry.value.getDate().equals(date)) {
                    result.add(entry.value);
                }
            });
        }

        return result;
    }
}
```

Open http://localhost:8080/dashboard.html

Add 2 more accounts to the producer and decrease the interval to 250ms