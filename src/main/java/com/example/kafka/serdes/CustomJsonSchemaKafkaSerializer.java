package com.example.kafka.serdes;

import com.example.kafka.util.StaticUtils;
import io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer;

import java.util.Map;

public class CustomJsonSchemaKafkaSerializer<T> extends JsonSchemaKafkaSerializer<T> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        setObjectMapper(StaticUtils.getCustomizedObjectMapper());

        super.configure(configs, isKey);
    }

}
