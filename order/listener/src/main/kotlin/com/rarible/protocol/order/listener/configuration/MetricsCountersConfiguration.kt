package com.rarible.protocol.order.listener.configuration

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.metric.OrderExpiredMetric
import com.rarible.protocol.order.listener.metric.OrderStartedMetric
import com.rarible.protocol.order.listener.metric.rarible.RaribleCancelEventMetric
import com.rarible.protocol.order.listener.metric.rarible.RaribleMatchEventMetric
import com.rarible.protocol.order.listener.metric.rarible.WrapperLooksrareMatchEventMetric
import com.rarible.protocol.order.listener.metric.rarible.WrapperSeaportMatchEventMetric
import com.rarible.protocol.order.listener.metric.rarible.WrapperX2Y2MatchEventMetric
import com.rarible.protocol.order.listener.metrics.sudoswap.SudoSwapCreatePairEventMetric
import com.rarible.protocol.order.listener.metrics.sudoswap.SudoSwapDepositNftEventMetric
import com.rarible.protocol.order.listener.metrics.sudoswap.SudoSwapInNftEventMetric
import com.rarible.protocol.order.listener.metrics.sudoswap.SudoSwapOutNftEventMetric
import com.rarible.protocol.order.listener.metrics.sudoswap.SudoSwapUpdateDeltaEventMetric
import com.rarible.protocol.order.listener.metrics.sudoswap.SudoSwapUpdateFeeEventMetric
import com.rarible.protocol.order.listener.metrics.sudoswap.SudoSwapUpdateSpotPriceEventMetric
import com.rarible.protocol.order.listener.metrics.sudoswap.SudoSwapWithdrawNftEventMetric
import com.rarible.protocol.order.listener.metrics.sudoswap.WrapperSudoSwapMatchEventMetric
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsCountersConfiguration(
    private val properties: OrderIndexerProperties,
    private val meterRegistry: MeterRegistry
) {
    /** Common metrics **/
    @Bean
    fun orderExpiredMetric(): RegisteredCounter {
        return OrderExpiredMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun orderStartedMetric(): RegisteredCounter {
        return OrderStartedMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    /** Rarible metrics **/
    @Bean
    fun raribleMatchEventMetric(): RegisteredCounter {
        return RaribleMatchEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun wrapperX2Y2MatchEventMetric(): RegisteredCounter {
        return WrapperX2Y2MatchEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun wrapperLooksrareMatchEventMetric(): RegisteredCounter {
        return WrapperLooksrareMatchEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun wrapperSeaportMatchEventMetric(): RegisteredCounter {
        return WrapperSeaportMatchEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    @Bean
    fun raribleCancelEventMetric(): RegisteredCounter {
        return RaribleCancelEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
    }

    /** SudoSwap */
    @Bean
    fun sudoSwapCreatePairEventCounter(): RegisteredCounter =
        SudoSwapCreatePairEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun sudoSwapUpdateDeltaEventCounter(): RegisteredCounter =
        SudoSwapUpdateDeltaEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun sudoSwapDepositNftEventCounter(): RegisteredCounter =
        SudoSwapDepositNftEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun sudoSwapUpdateFeeEventCounter(): RegisteredCounter =
        SudoSwapUpdateFeeEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun sudoSwapInNftEventCounter(): RegisteredCounter =
        SudoSwapInNftEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun sudoSwapOutNftEventCounter(): RegisteredCounter =
        SudoSwapOutNftEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun sudoSwapUpdateSpotPriceEventCounter(): RegisteredCounter =
        SudoSwapUpdateSpotPriceEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun sudoSwapWithdrawNftEventCounter(): RegisteredCounter =
        SudoSwapWithdrawNftEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)

    @Bean
    fun wrapperSudoSwapMatchEventCounter(): RegisteredCounter =
        WrapperSudoSwapMatchEventMetric(properties.metricRootPath, properties.blockchain).bind(meterRegistry)
}
