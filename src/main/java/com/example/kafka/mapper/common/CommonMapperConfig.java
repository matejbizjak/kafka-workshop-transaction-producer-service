package com.example.kafka.mapper.common;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingInheritanceStrategy;

/**
 * Common mapper configuration.
 */
@MapperConfig(
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    mappingInheritanceStrategy = MappingInheritanceStrategy.AUTO_INHERIT_FROM_CONFIG)
public interface CommonMapperConfig {}
