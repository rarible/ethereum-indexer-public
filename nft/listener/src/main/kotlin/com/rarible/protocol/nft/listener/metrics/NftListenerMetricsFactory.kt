package com.rarible.protocol.nft.listener.metrics

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.core.telemetry.metrics.LongGaugeMetric
import com.rarible.core.telemetry.metrics.Metric
import com.rarible.core.telemetry.metrics.Metric.Companion.tag
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class NftListenerMetricsFactory(
    private val properties: NftIndexerProperties,
    private val meterRegistry: MeterRegistry,
) {

    fun itemOwnershipConsistencyJobCheckedCounter(): RegisteredCounter {
        return countingMetric("item.ownership.consistency.job.checked")
            .bind(meterRegistry)
    }

    fun itemOwnershipConsistencyJobFixedCounter(): RegisteredCounter {
        return countingMetric("item.ownership.consistency.job.fixed")
            .bind(meterRegistry)
    }

    fun itemOwnershipConsistencyJobUnfixedCounter(): RegisteredCounter {
        return countingMetric("item.ownership.consistency.job.unfixed")
            .bind(meterRegistry)
    }

    fun ownershipItemConsistencyJobCheckedCounter(): RegisteredCounter {
        return countingMetric("ownership.item.consistency.job.checked")
            .bind(meterRegistry)
    }

    fun ownershipItemConsistencyJobFixedCounter(): RegisteredCounter {
        return countingMetric("ownership.item.consistency.job.fixed")
            .bind(meterRegistry)
    }

    fun ownershipItemConsistencyJobUnfixedCounter(): RegisteredCounter {
        return countingMetric("ownership.item.consistency.job.unfixed")
            .bind(meterRegistry)
    }

    fun itemOwnershipConsistencyJobDelayGauge(): RegisteredGauge<Long> {
        return object : LongGaugeMetric(
            name = "${properties.metricRootPath}.item.ownership.consistency.job.delay",
            tag("blockchain", properties.blockchain)
        ){}.bind(meterRegistry)
    }

    fun ownershipItemConsistencyJobDelayGauge(): RegisteredGauge<Long> {
        return object : LongGaugeMetric(
            name = "${properties.metricRootPath}.ownership.item.consistency.job.delay",
            tag("blockchain", properties.blockchain)
        ){}.bind(meterRegistry)
    }

    private fun countingMetric(suffix: String): CountingMetric {
        val root = properties.metricRootPath
        val blockchain = properties.blockchain
        return object : CountingMetric(
            "$root.$suffix", tag("blockchain", blockchain.value)
        ) {}
    }
}
