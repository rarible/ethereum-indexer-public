package com.rarible.protocol.order.listener.configuration

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.order.listener.misc.OpenSeaConverterErrorMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsCountersConfiguration(
    //private val properties: OrderIndexerProperties,
    private val meterRegistry: MeterRegistry
) {
    @Bean
    fun openSeaConverterErrorRegisteredCounter() : RegisteredCounter {
        return OpenSeaConverterErrorMetric("qwerty", Blockchain.ETHEREUM).bind(meterRegistry)
    }
}