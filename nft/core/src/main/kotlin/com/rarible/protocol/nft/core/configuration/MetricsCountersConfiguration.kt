package com.rarible.protocol.nft.core.configuration

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.nft.core.model.ErrorBurnActionMetric
import com.rarible.protocol.nft.core.model.ExecutedBurnActionMetric
import com.rarible.protocol.nft.core.model.IncomeBurnActionMetric
import com.rarible.protocol.nft.core.model.ItemDataQualityErrorMetric
import com.rarible.protocol.nft.core.model.ItemDataQualityJobRunMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsCountersConfiguration(
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
}
