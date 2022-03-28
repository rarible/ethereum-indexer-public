package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.item.ItemFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemDataQualityService(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val itemDataQualityErrorRegisteredCounter: RegisteredCounter,
    private val nftListenerProperties: NftListenerProperties,
    private val itemReduceService: ItemReduceService
) {
    fun checkItems(from: String?): Flow<String> {
        val filter = ItemFilterAll(
            sort = ItemFilter.Sort.LAST_UPDATE_DESC,
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
                ).toList()
                items.forEach { item ->
                    val ownershipsValue = getOwnershipsValue(item.id, nftListenerProperties.elementsFetchJobSize)
                    if(!checkItem(item)) {
                        val message = "Find potential data corruption for item ${item.id}, " +
                        "lastUpdateAt ${item.date}: supply=${item.supply}, " +
                                "ownershipsValue=$ownershipsValue."
                        logger.info("$message Try to fix.")
                        itemReduceService.update(item.token, item.tokenId).awaitFirstOrNull()
                        if(!checkItem(item)) {
                            itemDataQualityErrorRegisteredCounter.increment()
                            logger.warn("$message Can't be fixed.")
                        }
                    }
                    emit(ItemContinuation(item.date, item.id).toString())
                }
                continuation = items.lastOrNull()?.let { item -> ItemContinuation(item.date, item.id).toString() }
            } while (continuation != null)
        }
    }

    suspend fun checkItem(item: Item, showLog: Boolean = false):Boolean {
        val ownershipsValue = getOwnershipsValue(item.id, nftListenerProperties.elementsFetchJobSize)

        if(showLog) {
            println("Ownership: $ownershipsValue")
            println("Supply: ${item.supply}")
        }

        return ownershipsValue == item.supply

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