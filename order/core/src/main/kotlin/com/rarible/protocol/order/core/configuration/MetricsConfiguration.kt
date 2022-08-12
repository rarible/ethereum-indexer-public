package com.rarible.protocol.order.core.configuration

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.metric.ApprovalNotFoundMetric
import com.rarible.protocol.order.core.metric.RaribleOrderSaveMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfiguration(
    private val properties: OrderIndexerProperties,
    private val meterRegistry: MeterRegistry
) {
    @Bean
    fun approvalNotFoundCounter(): RegisteredCounter {
        return ApprovalNotFoundMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun raribleOrderSaveMetric(): RegisteredCounter {
        return RaribleOrderSaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }
}
