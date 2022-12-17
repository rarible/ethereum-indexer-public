package com.rarible.protocol.nft.listener.metrics

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class NftListenerMetricsFactory(
    private val properties: NftIndexerProperties,
    private val meterRegistry: MeterRegistry,
) {

    private val root = properties.metricRootPath
    private val blockchainTag = ImmutableTag("blockchain", properties.blockchain.value)

    val itemOwnershipConsistencyJobCheckedCounter: Counter = counter("item.ownership.consistency.job.checked")
    val itemOwnershipConsistencyJobFixedCounter: Counter = counter("item.ownership.consistency.job.fixed")
    val itemOwnershipConsistencyJobUnfixedCounter: Counter = counter("item.ownership.consistency.job.unfixed")

    val ownershipItemConsistencyJobCheckedCounter: Counter = counter("ownership.item.consistency.job.checked")
    val ownershipItemConsistencyJobFixedCounter: Counter = counter("ownership.item.consistency.job.fixed")
    val ownershipItemConsistencyJobUnfixedCounter: Counter = counter("ownership.item.consistency.job.unfixed")

    val itemOwnershipConsistencyJobDelayGauge: AtomicLong = gauge("$root.item.ownership.consistency.job.delay")
    val ownershipItemConsistencyJobDelayGauge: AtomicLong = gauge("$root.ownership.item.consistency.job.delay")

    // === InconsistentItemsRepair ===

    val inconsistentItemsRepairJobCheckedCounter: Counter = counter("inconsistent.items.repair.job.checked")
    val inconsistentItemsRepairJobFixedCounter: Counter = counter("inconsistent.items.repair.job.fixed")
    val inconsistentItemsRepairJobUnfixedCounter: Counter = counter("inconsistent.items.repair.job.unfixed")
    val inconsistentItemsRepairJobDelayGauge: AtomicLong = gauge("$root.inconsistent.items.repair.job.delay")

    // === Suspicious
    private val suspiciousCollectionCheckingCounter: AtomicLong = gauge("$root.suspicious.collection.checking")
    private val suspiciousItemCheckingCounter: Counter = counter("suspicious.item.checking")
    private val suspiciousItemFoundCounter: Counter = counter("suspicious.item.found")
    private val suspiciousItemUpdateCounter: Counter = counter("suspicious.item.update")

    fun onSuspiciousCollectionsGet(count: Int) {
        suspiciousCollectionCheckingCounter.set(count.toLong())
    }

    fun onSuspiciousItemsGet(count: Int) {
        suspiciousItemCheckingCounter.increment(count.toDouble())
    }

    fun onSuspiciousItemFound() {
        suspiciousItemFoundCounter.increment()
    }

    fun onSuspiciousItemUpdate() {
        suspiciousItemUpdateCounter.increment()
    }

    private fun gauge(name: String): AtomicLong {
        return meterRegistry.gauge(
            name,
            listOf(blockchainTag),
            AtomicLong(0)
        )!!
    }

    private fun counter(suffix: String): Counter {
        val root = properties.metricRootPath
        return meterRegistry.counter("$root.$suffix", listOf(blockchainTag))
    }
}
