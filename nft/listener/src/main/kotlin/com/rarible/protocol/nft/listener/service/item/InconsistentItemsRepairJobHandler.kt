package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.nft.core.misc.RateLimiter
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemContinuation
import com.rarible.protocol.nft.core.model.InconsistentItemContinuation.Companion.fromInconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemFilterAll
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.model.ItemProblemType
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.core.repository.inconsistentitem.InconsistentItemFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class InconsistentItemsRepairJobHandler(
    private val jobStateRepository: JobStateRepository,
    private val nftListenerProperties: NftListenerProperties,
    private val inconsistentItemRepository: InconsistentItemRepository,
    private val itemOwnershipConsistencyService: ItemOwnershipConsistencyService,
    @Qualifier("inconsistentItemsRepairRateLimiter") private val rateLimiter: RateLimiter,
    metricsFactory: NftListenerMetricsFactory,
) : JobHandler {

    companion object {

        private const val INCONSISTENT_ITEMS_REPAIR_JOB = "inconsistent-items-repair"
    }

    private val checkedCounter = metricsFactory.inconsistentItemsRepairJobCheckedCounter
    private val fixedCounter = metricsFactory.inconsistentItemsRepairJobFixedCounter
    private val unfixedCounter = metricsFactory.inconsistentItemsRepairJobUnfixedCounter
    private val delayMetric = metricsFactory.inconsistentItemsRepairJobDelayGauge

    private val properties = nftListenerProperties.inconsistentItemsRepair
    private val logger = LoggerFactory.getLogger(javaClass)
    private val filter = InconsistentItemFilterAll()

    override suspend fun handle() = coroutineScope {
        logger.info("InconsistentItemsRepairJobHandler handle() called, properties $properties")
        val itemsChannel = Channel<InconsistentItem>(properties.parallelism)
        val semaphore = Semaphore(properties.parallelism)
        repeat(properties.parallelism) { launchItemWorker(itemsChannel, semaphore) }
        val state = getState()
        try {
            do {
                val allItems = getItemsBatch(state.continuation)
                if (allItems.isEmpty()) {
                    state.latestChecked = Instant.now()
                    break
                }
                updateState(state, allItems.last())

                for (item in allItems) {
                    itemsChannel.send(item)
                }

                semaphore.waitUntilAvailable(properties.parallelism)
                saveState(state)
            } while (true)
        } finally {
            saveState(state)
            itemsChannel.close()
        }
    }

    private suspend fun getItemsBatch(continuation: String?): List<InconsistentItem> {
        return inconsistentItemRepository.search(
            filter.toCriteria(
                InconsistentItemContinuation.parse(continuation),
                nftListenerProperties.elementsFetchJobSize
            )
        ).toList()
    }

    private fun CoroutineScope.launchItemWorker(channel: ReceiveChannel<InconsistentItem>, semaphore: Semaphore) {
        launch {
            for (item in channel) {
                semaphore.acquire()
                processItem(item)
                checkedCounter.increment()
                semaphore.release()
            }
        }
    }

    private suspend fun processItem(item: InconsistentItem) {
        when (item.status!!) {
            InconsistentItemStatus.NEW -> processNew(item)
            InconsistentItemStatus.FIXED -> logger.info("Item ${item.id} is already fixed")
            InconsistentItemStatus.UNFIXED -> processUnfixed(item)
            InconsistentItemStatus.RELAPSED -> processRelapsed(item)
        }
    }

    private suspend fun processNew(item: InconsistentItem) {
        process(item)
    }

    private suspend fun processUnfixed(item: InconsistentItem) {
        process(item)
    }

    private suspend fun processRelapsed(item: InconsistentItem) {
        logger.info("InconsistentItem ${item.id} relapsedCount: ${item.relapseCount}")
        process(item)
    }

    private suspend fun process(initialItem: InconsistentItem) {
        var item = initialItem
        var attempts = 0
        var repeat = true

        do {
            val fixAttemptResult = applyFix(item)
            if (fixAttemptResult.fixVersionApplied != null) {
                attempts++
                item = item.copy(
                    fixVersionApplied = fixAttemptResult.fixVersionApplied
                )
                when (val checkResult = itemOwnershipConsistencyService.checkItem(item.id)) {
                    is ItemOwnershipConsistencyService.CheckResult.Success -> {
                        logger.info("InconsistentItem $item was fixed successfully")
                        saveWithStatus(item, InconsistentItemStatus.FIXED)
                        repeat = false
                    }
                    is ItemOwnershipConsistencyService.CheckResult.Failure -> {
                        item = item.copy(
                            type = checkResult.type,
                            supply = checkResult.supply,
                            ownerships = checkResult.ownerships,
                            supplyValue = checkResult.supply.value,
                            ownershipsValue = checkResult.ownerships.value,
                        )
                    }
                }
            } else {
                if (attempts > 0) {
                    logger.info("$attempts attempts were done, but ${item.id} is still unfixed")
                    saveWithStatus(item, InconsistentItemStatus.UNFIXED)
                } else {
                    logger.info("No attempts were done for ${item.id}, previous fix version applied: ${item.fixVersionApplied}")
                }
                repeat = false
            }
        } while (repeat)
    }

    // Currently fix for different problems is the same, but it may change in the future
    private suspend fun applyFix(item: InconsistentItem): ItemOwnershipConsistencyService.FixAttemptResult {
        rateLimiter.waitIfNecessary(1)
        logger.info("Applying fix for item ${item.id}, problem type ${item.type}")
        return when (item.type) {
            ItemProblemType.NOT_FOUND, ItemProblemType.SUPPLY_MISMATCH -> {
                itemOwnershipConsistencyService.tryFix(item.id, item.fixVersionApplied)
            }
        }
    }

    private suspend fun saveWithStatus(item: InconsistentItem, newStatus: InconsistentItemStatus) {
        inconsistentItemRepository.save(
            item.copy(
                status = newStatus,
            )
        )
        when (newStatus) {
            InconsistentItemStatus.FIXED -> fixedCounter.increment()
            InconsistentItemStatus.UNFIXED -> unfixedCounter.increment()
        }
    }

    private suspend fun saveState(state: InconsistentItemsRepairJobState) {
        logger.info("Saving state $state")
        jobStateRepository.save(INCONSISTENT_ITEMS_REPAIR_JOB, state)
        delayMetric.set((nowMillis().toEpochMilli() - state.latestChecked.toEpochMilli()))
    }

    private suspend fun updateState(state: InconsistentItemsRepairJobState, item: InconsistentItem) {
        state.latestChecked = item.lastUpdatedAt ?: Instant.EPOCH
        state.continuation = item.fromInconsistentItem().toString()
    }

    private suspend fun getState(): InconsistentItemsRepairJobState {
        return jobStateRepository.get(INCONSISTENT_ITEMS_REPAIR_JOB, InconsistentItemsRepairJobState::class.java)
            ?: InconsistentItemsRepairJobState()
    }

    private suspend fun Semaphore.waitUntilAvailable(permits: Int) {
        repeat(permits) { acquire() }
        repeat(permits) { release() }
    }

    data class InconsistentItemsRepairJobState(
        var continuation: String? = null,
        var latestChecked: Instant = Instant.EPOCH,
    )
}
