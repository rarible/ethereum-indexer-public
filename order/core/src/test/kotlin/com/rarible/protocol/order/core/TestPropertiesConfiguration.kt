package com.rarible.protocol.order.core

import com.rarible.ethereum.converters.StringToAddressConverter
import com.rarible.ethereum.converters.StringToBinaryConverter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestPropertiesConfiguration {

    @Bean
    @ConfigurationPropertiesBinding
    fun stringToAddressConverter() = StringToAddressConverter()

    @Bean
    @ConfigurationPropertiesBinding
    fun stringToBinaryConverter() = StringToBinaryConverter()

    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
}
