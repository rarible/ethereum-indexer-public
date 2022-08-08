package com.rarible.protocol.order.listener.configuration

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.metrics.looksrare.LooksrareCancelAllEventMetric
import com.rarible.protocol.order.listener.metrics.looksrare.LooksrareCancelOrdersEventMetric
import com.rarible.protocol.order.listener.metrics.looksrare.LooksrareOrderErrorMetric
import com.rarible.protocol.order.listener.metrics.looksrare.LooksrareOrderLoadMetric
import com.rarible.protocol.order.listener.metrics.looksrare.LooksrareOrderSaveMetric
import com.rarible.protocol.order.listener.metrics.looksrare.LooksrareTakeAskEventMetric
import com.rarible.protocol.order.listener.metrics.looksrare.LooksrareTakeBidEventMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderDelayLoadMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderDelaySaveMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderErrorMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderLoadMetric
import com.rarible.protocol.order.listener.misc.OpenSeaOrderSaveMetric
import com.rarible.protocol.order.listener.misc.SeaportCancelEventMetric
import com.rarible.protocol.order.listener.misc.SeaportCounterEventMetric
import com.rarible.protocol.order.listener.misc.SeaportEventErrorMetric
import com.rarible.protocol.order.listener.misc.SeaportFulfilledEventMetric
import com.rarible.protocol.order.listener.misc.SeaportOrderDelayMetric
import com.rarible.protocol.order.listener.misc.SeaportOrderErrorMetric
import com.rarible.protocol.order.listener.misc.SeaportOrderLoadMetric
import com.rarible.protocol.order.listener.misc.SeaportOrderSaveMetric
import com.rarible.protocol.order.listener.misc.X2Y2OrderCancelEventMetric
import com.rarible.protocol.order.listener.misc.X2Y2OrderLoadErrorMetric
import com.rarible.protocol.order.listener.misc.X2Y2OrderMatchEventMetric
import com.rarible.protocol.order.listener.misc.X2Y2OrderSaveMetric
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
    fun openSeaErrorCounter(): RegisteredCounter {
        return OpenSeaOrderErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaSaveCounter(): RegisteredCounter {
        return OpenSeaOrderSaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaLoadCounter(): RegisteredCounter {
        return OpenSeaOrderLoadMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaDelaySaveCounter(): RegisteredCounter {
        return OpenSeaOrderDelaySaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun openSeaDelayLoadCounter(): RegisteredCounter {
        return OpenSeaOrderDelayLoadMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    /** Seaport metrics **/
    @Bean
    fun seaportErrorCounter(): RegisteredCounter {
        return SeaportOrderErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportSaveCounter(): RegisteredCounter {
        return SeaportOrderSaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportLoadCounter(): RegisteredCounter {
        return SeaportOrderLoadMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportEventErrorCounter(): RegisteredCounter {
        return SeaportEventErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportFulfilledEventCounter(): RegisteredCounter {
        return SeaportFulfilledEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportCancelEventCounter(): RegisteredCounter {
        return SeaportCancelEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportCounterEventCounter(): RegisteredCounter {
        return SeaportCounterEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun seaportOrderDelayGauge(): RegisteredGauge<Long> {
        return SeaportOrderDelayMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    /** Looksrare metrics **/
    @Bean
    fun looksrareErrorCounter(): RegisteredCounter {
        return LooksrareOrderErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun looksrareSaveCounter(): RegisteredCounter {
        return LooksrareOrderSaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun looksrareLoadCounter(): RegisteredCounter {
        return LooksrareOrderLoadMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun looksrareTakeAskEventMetric(): RegisteredCounter {
        return LooksrareTakeAskEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun looksrareTakeBidEventMetric(): RegisteredCounter {
        return LooksrareTakeBidEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun looksrareCancelOrdersEventMetric(): RegisteredCounter {
        return LooksrareCancelOrdersEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun looksrareCancelAllEventMetric(): RegisteredCounter {
        return LooksrareCancelAllEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    /** X2Y2 */
    @Bean
    fun x2y2OrderSaveCounter(): RegisteredCounter =
        X2Y2OrderSaveMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun x2y2OrderLoadErrorCounter(): RegisteredCounter =
        X2Y2OrderLoadErrorMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun x2y2OrderCancelEventCounter(): RegisteredCounter =
        X2Y2OrderCancelEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun x2y2OrderMatchEventCounter(): RegisteredCounter =
        X2Y2OrderMatchEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
}
