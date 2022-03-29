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
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ItemDataQualityService(
    private val itemRepository: ItemRepository,
    private val ownershipRepository: OwnershipRepository,
    private val itemDataQualityErrorRegisteredCounter: RegisteredCounter,
    private val nftListenerProperties: NftListenerProperties,
    private val itemReduceService: ItemReduceService,
    private val mongo: ReactiveMongoOperations
) {
    fun checkItems(from: String?, dropCollection: Boolean = true): Flow<String> {
        val filter = ItemFilterAll(
            sort = ItemFilter.Sort.LAST_UPDATE_DESC,
            showDeleted = false
        )
        return flow {
            initializeCollection(dropCollection)
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

                        if(!checkItem(item,fromRepository = true, writeToBd = true)) {
                            itemDataQualityErrorRegisteredCounter.increment()
                            logger.warn("$message Can't be fixed.")
                        } else {
                            logger.info("Item ${item.id}, lastUpdateAt ${item.date}: supply=${item.supply} was fixed")
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
            mongo.dropCollection(COLLECTION).awaitFirstOrNull()
        }
        if(!mongo.collectionExists(COLLECTION).awaitFirst()) {
            mongo.createCollection(COLLECTION).awaitFirst()
        }
    }

    suspend fun checkItem(item: Item, fromRepository: Boolean = false, writeToBd: Boolean = false): Boolean {
        var updatedItem = item
        if (fromRepository) {
            val repositoryItem = itemRepository.findById(ItemId(item.token, item.tokenId)).awaitFirstOrNull()
            repositoryItem ?: return false
            updatedItem = repositoryItem
        }
        val ownershipsValue = getOwnershipsValue(updatedItem.id, nftListenerProperties.elementsFetchJobSize)
        val result = ownershipsValue == updatedItem.supply
        if (!result && writeToBd) {
            mongo.save(InconsistentItems(item.token, item.tokenId, item.supply, ownershipsValue), COLLECTION)
                .awaitFirstOrNull()
        }
        return result
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
        const val COLLECTION = "inconsistent_items"
        private val logger = LoggerFactory.getLogger(ItemDataQualityService::class.java)

        data class InconsistentItems(
            val token: Address,
            val tokenId: EthUInt256,
            val supply: EthUInt256,
            val supplyOwnership: EthUInt256
        )
    }

}

