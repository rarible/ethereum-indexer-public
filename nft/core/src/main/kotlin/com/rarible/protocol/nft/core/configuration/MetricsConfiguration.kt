package com.rarible.protocol.nft.core.configuration

import com.rarible.blockchain.scanner.monitoring.BlockchainMonitor
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.client.monitoring.MonitoringCallback
import com.rarible.protocol.nft.core.model.ErrorBurnActionMetric
import com.rarible.protocol.nft.core.model.ExecutedBurnActionMetric
import com.rarible.protocol.nft.core.model.IncomeBurnActionMetric
import com.rarible.protocol.nft.core.model.ItemDataQualityErrorMetric
import com.rarible.protocol.nft.core.model.ItemDataQualityJobRunMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
class MetricsConfiguration(
    private val properties: NftIndexerProperties,
    private val meterRegistry: MeterRegistry
) {
    @Bean
    @Qualifier("ItemDataQualityErrorRegisteredCounter")
    fun itemDataQualityErrorRegisteredCounter(): RegisteredCounter {
        return ItemDataQualityErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    @Qualifier("ItemDataQualityJobRunRegisteredCounter")
    fun itemDataQualityJobRunRegisteredCounter(): RegisteredCounter {
        return ItemDataQualityJobRunMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    @Qualifier("IncomeBurnActionCounter")
    fun incomeBurnActionMetric(): RegisteredCounter {
        return IncomeBurnActionMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    @Qualifier("ExecutedBurnActionCounter")
    fun executedBurnActionMetric(): RegisteredCounter {
        return ExecutedBurnActionMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    @Qualifier("ErrorBurnActionMetric")
    fun errorBurnActionMetric(): RegisteredCounter {
        return ErrorBurnActionMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
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
