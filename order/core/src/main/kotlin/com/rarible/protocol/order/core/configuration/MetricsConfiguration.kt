package com.rarible.protocol.order.core.configuration

import com.rarible.blockchain.scanner.monitoring.BlockchainMonitor
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.client.monitoring.MonitoringCallback
import com.rarible.protocol.order.core.metric.RaribleOrderSaveMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
class MetricsConfiguration(
    private val properties: OrderIndexerProperties,
    private val meterRegistry: MeterRegistry
) {
    @Bean
    fun raribleOrderSaveMetric(): RegisteredCounter {
        return RaribleOrderSaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    @ConditionalOnMissingBean(MonitoringCallback::class)
    fun ethereumMonitoringCallback(): MonitoringCallback {
        val monitor = BlockchainMonitor(meterRegistry)
        return object : MonitoringCallback {
            override fun <T> onBlockchainCall(method: String, monoCall: () -> Mono<T>) =
                monitor.onBlockchainCall(properties.blockchain.value, method, monoCall)
        }
    }
}
