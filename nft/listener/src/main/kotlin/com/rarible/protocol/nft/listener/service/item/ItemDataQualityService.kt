package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.item.ItemFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemDataQualityService(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val itemDataQualityErrorRegisteredCounter: RegisteredCounter,
    private val nftListenerProperties: NftListenerProperties
) {
    fun checkItems(from: String?): Flow<String> {
        val filter = ItemFilterAll(
            sort = ItemFilter.Sort.LAST_UPDATE_ASC,
            showDeleted = false
        )
        return flow {
            var continuation = from
            do {
                val items = itemRepository.search(
                    filter.toCriteria(
                        ItemContinuation.parse(continuation),
                        nftListenerProperties.elementsFetchJobSize
                    )
                )
                items.forEach { item ->
                    val ownershipsValue = getOwnershipsValue(item.id, nftListenerProperties.elementsFetchJobSize)
                    if (ownershipsValue != item.supply) {
                        logger.info(
                            "Find potential data corruption for item ${item.id}: supply=${item.supply}, ownershipsValue=$ownershipsValue"
                        )
                        itemDataQualityErrorRegisteredCounter.increment()
                    }
                    emit(ItemContinuation(item.date, item.id).toString())
                }
                continuation = items.lastOrNull()?.let { item -> ItemContinuation(item.date, item.id).toString() }
            } while (continuation != null)
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

    companion object {
        private val logger = LoggerFactory.getLogger(ItemDataQualityService::class.java)
    }
}