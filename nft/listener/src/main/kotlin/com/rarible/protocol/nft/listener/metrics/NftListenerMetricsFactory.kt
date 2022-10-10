package com.rarible.protocol.nft.listener.metrics

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.core.telemetry.metrics.RegisteredCounter
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
        return ItemOwnershipConsistencyJobCheckedMetric(properties.metricRootPath, properties.blockchain)
            .bind(meterRegistry)
    }

    fun itemOwnershipConsistencyJobFixedCounter(): RegisteredCounter {
        return ItemOwnershipConsistencyJobFixedMetric(properties.metricRootPath, properties.blockchain)
            .bind(meterRegistry)
    }

    fun itemOwnershipConsistencyJobUnfixedCounter(): RegisteredCounter {
        return ItemOwnershipConsistencyJobUnfixedMetric(properties.metricRootPath, properties.blockchain)
            .bind(meterRegistry)
    }

    private class ItemOwnershipConsistencyJobCheckedMetric(root: String, blockchain: Blockchain) : CountingMetric(
        "$root.item.ownership.consistency.job.checked", tag("blockchain", blockchain.value)
    )

    private class ItemOwnershipConsistencyJobFixedMetric(root: String, blockchain: Blockchain) : CountingMetric(
        "$root.item.ownership.consistency.job.fixed", tag("blockchain", blockchain.value)
    )

    private class ItemOwnershipConsistencyJobUnfixedMetric(root: String, blockchain: Blockchain) : CountingMetric(
        "$root.item.ownership.consistency.job.unfixed", tag("blockchain", blockchain.value)
    )
}
