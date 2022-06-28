package com.rarible.protocol.order.listener.configuration

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.misc.OpenSeaOrderDelayLoadMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderDelaySaveMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderErrorMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderLoadMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderSaveMetric
import com.rarible.protocol.order.listener.misc.SeaportEventErrorMetric
import com.rarible.protocol.order.listener.misc.SeaportOrderErrorMetric
import com.rarible.protocol.order.listener.misc.SeaportOrderLoadMetric
import com.rarible.protocol.order.listener.misc.SeaportOrderSaveMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsCountersConfiguration(
    private val properties: OrderIndexerProperties,
    private val meterRegistry: MeterRegistry
) {
    /** OpenSea metrics **/
    @Bean
    fun openSeaErrorCounter() : RegisteredCounter {
        return OpenSeaOrderErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaSaveCounter() : RegisteredCounter {
        return OpenSeaOrderSaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaLoadCounter() : RegisteredCounter {
        return OpenSeaOrderLoadMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaDelaySaveCounter() : RegisteredCounter {
        return OpenSeaOrderDelaySaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaDelayLoadCounter() : RegisteredCounter {
        return OpenSeaOrderDelayLoadMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    /** Seaport metrics **/
    @Bean
    fun seaportErrorCounter() : RegisteredCounter {
        return SeaportOrderErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportSaveCounter() : RegisteredCounter {
        return SeaportOrderSaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportLoadCounter() : RegisteredCounter {
        return SeaportOrderLoadMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportEventErrorCounter() : RegisteredCounter {
        return SeaportEventErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }
}