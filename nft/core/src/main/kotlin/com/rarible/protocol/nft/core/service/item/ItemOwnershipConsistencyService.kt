package com.rarible.protocol.nft.core.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProblemType
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterByItem
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipFilterCriteria.toCriteria
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ItemOwnershipConsistencyService(
    private val itemReduceService: ItemReduceService,
    private val ownershipRepository: OwnershipRepository,
    private val itemRepository: ItemRepository,
) {

    companion object {
        const val CURRENT_FIX_VERSION = 2
        private const val elementsFetchSize = 1000
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun checkItem(itemId: ItemId): CheckResult {
        val item = itemRepository.findById(itemId).awaitFirstOrNull()
            ?: return CheckResult.Failure(ItemProblemType.NOT_FOUND, EthUInt256.ZERO, EthUInt256.ZERO)
        return checkItem(item)
    }

    suspend fun checkItem(item: Item): CheckResult {
        logger.info("Checking item<->ownership consistency for item ${item.id}")
        val ownershipsSupply = getOwnershipsTotalSupply(item.id, elementsFetchSize)
        return if (ownershipsSupply == item.supply) {
            logger.info("Consistency check passed for item ${item.id}, supply: ${item.supply}")
            CheckResult.Success
        } else {
            logger.warn("Consistency check failed for item ${item.id}, " +
                    "item supply: ${item.supply}, ownerships supply: $ownershipsSupply")
            CheckResult.Failure(ItemProblemType.SUPPLY_MISMATCH, item.supply, ownershipsSupply)
        }
    }

    suspend fun tryFix(item: Item, deleteOwnerships: Boolean = false): FixAttemptResult {
        return tryFix(item.id, deleteOwnerships)
    }

    suspend fun tryFix(itemId: ItemId, deleteOwnerships: Boolean = false): FixAttemptResult {
        logger.info("Attempting to fix item<->ownership consistency for item $itemId")
        if (deleteOwnerships) {
            val deleted = ownershipRepository.deleteAllByItemId(itemId = itemId).asFlow().toList()
            logger.info("Deleted ${deleted.size} ownerships")
        }
        itemReduceService.update(itemId.token, itemId.tokenId).awaitFirstOrNull()
        logger.info("Attempt finished for item $itemId")
        val item = itemRepository.findById(itemId).awaitSingleOrNull()
        return FixAttemptResult(itemId, item, CURRENT_FIX_VERSION)
    }

    private suspend fun getOwnershipsTotalSupply(itemId: ItemId, limit: Int): EthUInt256 {
        var value = EthUInt256.ZERO
        var continuation: OwnershipContinuation? = null
        do {
            val filter = OwnershipFilterByItem(
                contract = itemId.token,
                tokenId = itemId.tokenId.value,
                sort = OwnershipFilter.Sort.LAST_UPDATE
            )
            val ownerships = ownershipRepository.search(filter.toCriteria(continuation = continuation, limit = limit))
            continuation = if (ownerships.size < limit) null else ownerships.last()
                .let { OwnershipContinuation(it.date, it.id) }
            value = ownerships.fold(value) { acc, ownership -> acc + ownership.value }
        } while (continuation != null)

        return value
    }

    sealed class CheckResult {
        object Success : CheckResult()

        data class Failure(
            val type: ItemProblemType,
            val supply: EthUInt256,
            val ownerships: EthUInt256
        ) : CheckResult()
    }

    data class FixAttemptResult(
        val itemId: ItemId,
        val item: Item?,
        val fixVersion: Int,
    )
}
