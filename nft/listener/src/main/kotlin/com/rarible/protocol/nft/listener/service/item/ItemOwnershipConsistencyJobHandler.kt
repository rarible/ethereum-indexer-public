package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemContinuation
import com.rarible.protocol.nft.core.model.ItemFilter
import com.rarible.protocol.nft.core.model.ItemFilterAll
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService.CheckResult.Failure
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService.CheckResult.Success
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

const val ITEM_OWNERSHIP_CONSISTENCY_JOB = "item-ownership-consistency"

@Component
class ItemOwnershipConsistencyJobHandler(
    private val jobStateRepository: JobStateRepository,
    private val itemRepository: ItemRepository,
    private val nftListenerProperties: NftListenerProperties,
    private val itemOwnershipConsistencyService: ItemOwnershipConsistencyService,
    private val inconsistentItemRepository: InconsistentItemRepository,
) : JobHandler {

    private val properties = nftListenerProperties.itemOwnershipConsistency
    private val logger = LoggerFactory.getLogger(javaClass)
    private val filter = ItemFilterAll(
        sort = ItemFilter.Sort.LAST_UPDATE_ASC,
        showDeleted = false,
    )

    override suspend fun handle() {
        logger.info("ItemOwnershipConsistencyHandler handle() called")
        val state = getState()
        try {
            do {
                val items = getItemsBatch(state.continuation)
                logger.info("Got ${items.size} items to check")
                if (items.isEmpty()) {
                    state.continuation = null
                    state.latestChecked = Instant.now()
                    break
                }

                items.forEach { item -> checkAndFixItem(item) }

                state.latestChecked = items.last().date
                state.continuation = items.last().let { item -> ItemContinuation(item.date, item.id).toString() }

                saveState(state)
            } while (state.latestChecked < Instant.now().minus(properties.checkTimeOffset))
        } finally {
            saveState(state)
        }
    }

    private suspend fun getItemsBatch(continuation: String?): List<Item> {
        return itemRepository.search(
            filter.toCriteria(
                ItemContinuation.parse(continuation),
                nftListenerProperties.elementsFetchJobSize
            )
        ).toList()
    }

    private suspend fun checkAndFixItem(item: Item) {
        var checkResult = itemOwnershipConsistencyService.checkItem(item)
        when (checkResult) {
            is Failure -> {
                if (!properties.autofix) return

                var fixedItem = itemOwnershipConsistencyService.tryFix(item)
                checkResult = itemOwnershipConsistencyService.checkItem(fixedItem)
                when (checkResult) {
                    is Failure -> {
                        inconsistentItemRepository.save(
                            InconsistentItem(
                                token = fixedItem.token,
                                tokenId = fixedItem.tokenId,
                                supply = checkResult.supply,
                                ownerships = checkResult.ownerships,
                                supplyValue = checkResult.supply.value.toLong(),
                                ownershipsValue = checkResult.ownerships.value.toLong()
                            )
                        )
                    }

                    Success -> {
                        logger.info("Item ${item.id} ownership consistency was fixed successfully")
                    }
                }
            }

            Success -> {
                // Do nothing, item<->ownerships is consistent, info logged
            }
        }
    }

    private suspend fun getState(): ItemOwnershipConsistencyJobState {
        return jobStateRepository.get(ITEM_OWNERSHIP_CONSISTENCY_JOB, ItemOwnershipConsistencyJobState::class.java)
            ?: ItemOwnershipConsistencyJobState()
    }

    private suspend fun saveState(state: ItemOwnershipConsistencyJobState) {
        jobStateRepository.save(ITEM_OWNERSHIP_CONSISTENCY_JOB, state)
    }

    data class ItemOwnershipConsistencyJobState(
        var continuation: String? = null,
        var latestChecked: Instant = Instant.EPOCH,
    )
}