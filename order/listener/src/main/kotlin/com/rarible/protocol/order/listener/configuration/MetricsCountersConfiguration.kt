package com.rarible.protocol.order.listener.configuration

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.misc.OpenSeaOrderDelayLoadMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderDelaySaveMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderErrorMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderLoadMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderSaveMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsCountersConfiguration(
    private val properties: OrderIndexerProperties,
    private val meterRegistry: MeterRegistry
) {
    @Bean
    fun openSeaOrderErrorRegisteredCounter() : RegisteredCounter {
        return OpenSeaOrderErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaOrderSaveRegisteredCounter() : RegisteredCounter {
        return OpenSeaOrderSaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaOrderLoadRegisteredCounter() : RegisteredCounter {
        return OpenSeaOrderLoadMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaOrderDelaySaveRegisteredCounter() : RegisteredCounter {
        return OpenSeaOrderDelaySaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaOrderDelayLoadRegisteredCounter() : RegisteredCounter {
        return OpenSeaOrderDelayLoadMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }
}