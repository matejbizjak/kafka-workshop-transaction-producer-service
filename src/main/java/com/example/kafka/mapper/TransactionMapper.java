package com.example.kafka.mapper;

import com.example.kafka.mapper.common.CommonMapperConfig;
import com.example.kafka.messaging.TransactionEvent;
import com.example.kafka.model.Transaction;
import org.mapstruct.Mapper;

@Mapper(config = CommonMapperConfig.class)
public interface TransactionMapper {

    TransactionEvent dtoToEvent(Transaction dto);
}
