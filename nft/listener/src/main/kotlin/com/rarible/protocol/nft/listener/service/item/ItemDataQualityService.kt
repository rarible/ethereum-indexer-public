package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemContinuation
import com.rarible.protocol.nft.core.model.ItemFilter
import com.rarible.protocol.nft.core.model.ItemFilterAll
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterByItem
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.item.ItemFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@Deprecated("Use ItemOwnershipConsistencyService and ItemOwnershipConsistencyJobHandler")
class ItemDataQualityService(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val itemDataQualityErrorRegisteredCounter: RegisteredCounter,
    private val nftListenerProperties: NftListenerProperties,
    private val itemReduceService: ItemReduceService,
    private val inconsistentItemRepository: InconsistentItemRepository
) {
    fun checkItems(from: String?): Flow<String> {
        return flow {
            initializeCollection(dropInconsistentCollection = from == null)

            val filter = ItemFilterAll(
                sort = ItemFilter.Sort.LAST_UPDATE_DESC,
                showDeleted = false
            )

            var continuation = from
            do {
                val items = itemRepository.search(
                    filter.toCriteria(
                        ItemContinuation.parse(continuation),
                        nftListenerProperties.elementsFetchJobSize
                    )
                ).toList()

                items.forEach { item ->
                    when (val checkResult = checkItem(item)) {
                        is CheckResult.Success -> {}
                        is CheckResult.Fail -> {
                            logger.info("Try fix item ${item.id.stringValue}, supply=${checkResult.supply}, ownerships=${checkResult.ownerships}")
                            itemReduceService.update(item.token, item.tokenId).awaitFirstOrNull()
                            var fixedItem = itemRepository.findById(item.id).awaitSingle()
                            when (val check = checkItem(fixedItem)) {
                                is CheckResult.Success -> {
                                    logger.info("Item ${fixedItem.id} was fixed")
                                }
                                is CheckResult.Fail -> {
                                    logger.warn("Can't fix item by simple reduce ${item.id.stringValue}, supply=${check.supply}, ownerships=${check.ownerships}, try to reduce after delete ownerships ...")
                                    val deleted = ownershipRepository.deleteAllByItemId(fixedItem.id).asFlow().toList()
                                    logger.info("Deleted ${deleted.size} ownerships ...")
                                    itemReduceService.update(item.token, item.tokenId).awaitFirstOrNull()
                                    fixedItem = itemRepository.findById(item.id).awaitSingle()
                                    when (val afterDeleteCheck = checkItem(fixedItem)) {
                                        is CheckResult.Success -> {
                                            logger.info("Item ${fixedItem.id} was fixed after ownership's deleted")
                                        }

                                        is CheckResult.Fail -> {
                                            logger.warn("Can't fix item after deleting ownerships ${item.id.stringValue}, supply=${afterDeleteCheck.supply}, ownerships=${afterDeleteCheck.ownerships}")
                                            inconsistentItemRepository.save(
                                                InconsistentItem(
                                                    token = item.token,
                                                    tokenId = item.tokenId,
                                                    supply = afterDeleteCheck.supply,
                                                    ownerships = afterDeleteCheck.ownerships,
                                                    supplyValue = afterDeleteCheck.supply.value,
                                                    ownershipsValue = afterDeleteCheck.ownerships.value,
                                                    fixVersionApplied = 1,
                                                    lastUpdatedAt = nowMillis(),
                                                    status = InconsistentItemStatus.UNFIXED,
                                                )
                                            )
                                            itemDataQualityErrorRegisteredCounter.increment()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    emit(ItemContinuation(item.date, item.id).toString())
                }
                continuation = items.lastOrNull()?.let { item -> ItemContinuation(item.date, item.id).toString() }
            } while (continuation != null)
        }
    }

    private suspend fun initializeCollection(dropInconsistentCollection: Boolean) {
        if (dropInconsistentCollection) {
            inconsistentItemRepository.dropCollection()
        }
    }

    suspend fun checkItem(item: Item): CheckResult {
        val ownerships = getOwnershipsValue(item.id, nftListenerProperties.elementsFetchJobSize)
        return if (ownerships == item.supply) {
            CheckResult.Success
        } else {
            CheckResult.Fail(supply = item.supply, ownerships = ownerships)
        }
    }

    private suspend fun getOwnershipsValue(itemId: ItemId, limit: Int): EthUInt256 {
        var value = EthUInt256.ZERO
        var continuation: OwnershipContinuation? = null
        do {
            val filter = OwnershipFilterByItem(
                contract = itemId.token,
                tokenId = itemId.tokenId.value,
                sort = OwnershipFilter.Sort.LAST_UPDATE
            )
            val ownerships = ownershipRepository.search(filter.toCriteria(continuation = continuation, limit = limit))
            continuation = if (ownerships.isEmpty() || ownerships.size < limit) null else ownerships.last().let { OwnershipContinuation(it.date, it.id) }
            value = ownerships.fold(value) { acc, ownership -> acc + ownership.value }
        } while (continuation != null)

        return value
    }

    sealed class CheckResult {
        object Success : CheckResult()

        data class Fail(
            val supply: EthUInt256,
            val ownerships: EthUInt256
        ) : CheckResult()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItemDataQualityService::class.java)
    }
}


