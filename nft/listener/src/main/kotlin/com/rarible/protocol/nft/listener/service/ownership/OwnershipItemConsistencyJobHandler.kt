package com.rarible.protocol.nft.listener.service.ownership

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProblemType
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterAll
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService.CheckResult.Failure
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService.CheckResult.Success
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

const val OWNERSHIP_ITEM_CONSISTENCY_JOB = "ownership-item-consistency"

@Component
class OwnershipItemConsistencyJobHandler(
    private val jobStateRepository: JobStateRepository,
    private val ownershipRepository: OwnershipRepository,
    private val itemRepository: ItemRepository,
    private val inconsistentItemRepository: InconsistentItemRepository,
    private val nftListenerProperties: NftListenerProperties,
    private val itemOwnershipConsistencyService: ItemOwnershipConsistencyService,
    metricsFactory: NftListenerMetricsFactory,
) : JobHandler {

    private val checkedCounter = metricsFactory.ownershipItemConsistencyJobCheckedCounter
    private val fixedCounter = metricsFactory.ownershipItemConsistencyJobFixedCounter
    private val unfixedCounter = metricsFactory.ownershipItemConsistencyJobUnfixedCounter
    private val delayMetric = metricsFactory.ownershipItemConsistencyJobDelayGauge

    private val properties = nftListenerProperties.ownershipItemConsistency
    private val logger = LoggerFactory.getLogger(javaClass)
    private val filter = OwnershipFilterAll(
        sort = OwnershipFilter.Sort.LAST_UPDATE_ASC,
        showDeleted = false,
    )

    override suspend fun handle() = coroutineScope {
        logger.info("OwnershipItemConsistencyHandler handle() called, properties $properties")
        val ownershipChannel = Channel<Ownership>(properties.parallelism)
        val semaphore = Semaphore(properties.parallelism)
        repeat(properties.parallelism) { launchItemWorker(ownershipChannel, semaphore) }
        val state = getState()
        try {
            do {
                val timeThreshold = Instant.now().minus(properties.checkTimeOffset)

                val allOwnerships = ownershipRepository.search(
                    filter.toCriteria(
                        OwnershipContinuation.parse(state.continuation),
                        nftListenerProperties.elementsFetchJobSize
                    )
                ).filter { it.date < timeThreshold }

                if (allOwnerships.isEmpty()) {
                    state.latestChecked = timeThreshold
                    break
                }
                updateState(state, allOwnerships.last())

                val ownershipsByItemId = allOwnerships.associateBy { ItemId(it.token, it.tokenId) }
                val inconsistentItemIds = inconsistentItemRepository.searchByIds(ownershipsByItemId.keys)
                    .filter { it.status != InconsistentItemStatus.FIXED }
                    .map { it.id }
                    .toSet()
                val itemIdsToCheck = ownershipsByItemId.keys - inconsistentItemIds
                logger.info("Got ${itemIdsToCheck.size} ownerships to check (${inconsistentItemIds.size} skipped as already inconsistent)")
                val foundItemIds = itemRepository.searchByIds(itemIdsToCheck).map { it.id }.toSet()
                val notFoundItems = itemIdsToCheck - foundItemIds
                val ownershipsToFix = ownershipsByItemId.filterKeys { it in notFoundItems }.values
                logger.info("Got ${ownershipsToFix.size} ownerships to fix")

                for (ownership in ownershipsToFix) {
                    ownershipChannel.send(ownership)
                }

                semaphore.waitUntilAvailable(properties.parallelism)
                saveState(state)
                if (itemIdsToCheck.isNotEmpty()) checkedCounter.increment(itemIdsToCheck.size.toDouble())
            } while (true)
        } finally {
            saveState(state)
            ownershipChannel.close()
        }
    }

    private suspend fun updateState(state: OwnershipItemConsistencyJobState, ownership: Ownership) {
        state.latestChecked = ownership.date
        state.continuation = OwnershipContinuation(ownership.date, ownership.id).toString()
    }

    private suspend fun getState(): OwnershipItemConsistencyJobState {
        return jobStateRepository.get(OWNERSHIP_ITEM_CONSISTENCY_JOB, OwnershipItemConsistencyJobState::class.java)
            ?: OwnershipItemConsistencyJobState()
    }

    private suspend fun saveState(state: OwnershipItemConsistencyJobState) {
        logger.info("Saving state $state")
        jobStateRepository.save(OWNERSHIP_ITEM_CONSISTENCY_JOB, state)
        delayMetric.set((nowMillis().toEpochMilli() - state.latestChecked.toEpochMilli()))
    }

    private fun CoroutineScope.launchItemWorker(channel: ReceiveChannel<Ownership>, semaphore: Semaphore) {
        launch {
            for (ownership in channel) {
                semaphore.acquire()
                fixThenCheckOwnership(ownership)
                semaphore.release()
            }
        }
    }

    private suspend fun fixThenCheckOwnership(ownership: Ownership) {
        val itemId = ItemId(ownership.token, ownership.tokenId)
        if (!properties.autofix) {
            saveToInconsistentItems(itemId, checkResult = null, found = false, triedToFix = false)
            return
        }

        logger.info("Fixing ownership ${ownership.id} consistency (itemId: $itemId)")
        val fixedItem = itemOwnershipConsistencyService.tryFix(itemId).item
        if (fixedItem == null) {
            saveToInconsistentItems(itemId, checkResult = null, found = false, triedToFix = true)
        } else {
            when (val checkResult = itemOwnershipConsistencyService.checkItem(fixedItem)) {
                is Failure -> {
                    saveToInconsistentItems(itemId, checkResult, found = true, triedToFix = true)
                }

                Success -> {
                    fixedCounter.increment()
                    logger.info("Ownership ${ownership.id} item ${fixedItem.id} consistency was fixed successfully")
                }
            }
        }
    }

    private suspend fun saveToInconsistentItems(
        itemId: ItemId,
        checkResult: Failure?,
        found: Boolean,
        triedToFix: Boolean
    ) {
        if (inconsistentItemRepository.insert(
                InconsistentItem(
                    token = itemId.token,
                    tokenId = itemId.tokenId,
                    type = if (found) ItemProblemType.SUPPLY_MISMATCH else ItemProblemType.NOT_FOUND,
                    supply = checkResult?.supply,
                    ownerships = checkResult?.ownerships,
                    supplyValue = checkResult?.supply?.value,
                    ownershipsValue = checkResult?.ownerships?.value,
                    fixVersionApplied = if (triedToFix) 2 else null,
                    status = if (triedToFix) InconsistentItemStatus.UNFIXED else InconsistentItemStatus.NEW,
                    lastUpdatedAt = nowMillis(),
                )
            )
        ) {
            logger.info("Saved $itemId to inconsistent_items")
            unfixedCounter.increment()
        } else {
            logger.info("Couldn't save $itemId to inconsistent_items, as it's already there")
        }
    }

    private suspend fun Semaphore.waitUntilAvailable(permits: Int) {
        repeat(permits) { acquire() }
        repeat(permits) { release() }
    }

    data class OwnershipItemConsistencyJobState(
        var continuation: String? = null,
        var latestChecked: Instant = Instant.EPOCH,
    )
}
