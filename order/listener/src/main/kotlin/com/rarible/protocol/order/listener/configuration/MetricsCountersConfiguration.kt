package com.rarible.protocol.order.listener.configuration

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.misc.OpenSeaConverterErrorMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsCountersConfiguration(
    private val properties: OrderIndexerProperties,
    private val meterRegistry: MeterRegistry
) {
    @Bean
    fun openSeaConverterErrorRegisteredCounter() : RegisteredCounter {
        return OpenSeaConverterErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }
}