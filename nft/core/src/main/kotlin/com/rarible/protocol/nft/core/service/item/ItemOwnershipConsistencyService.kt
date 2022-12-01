package com.rarible.protocol.nft.core.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProblemType
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterByItem
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.ownership.OwnershipService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipUpdateService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ItemOwnershipConsistencyService(
    private val itemReduceService: ItemReduceService,
    private val ownershipService: OwnershipService,
    private val ownershipUpdateService: OwnershipUpdateService,
    private val itemRepository: ItemRepository,
) {

    companion object {
        private const val elementsFetchSize = 1000
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private val fixAttemptMethods = listOf(
        FixAttemptMethod(
            fixVersion = 2,
            weight = 10,
            method = { itemId -> this.fixInternal(itemId, false) }
        ),
        FixAttemptMethod(
            fixVersion = 3,
            weight = 20,
            method = { itemId -> this.fixInternal(itemId, true) }
        )
    ).sortedBy { it.weight }

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

    suspend fun tryFix(item: Item, previousFixVersion: Int? = null): FixAttemptResult {
        return tryFix(item.id, previousFixVersion)
    }

    suspend fun tryFix(itemId: ItemId, previousFixVersion: Int? = null): FixAttemptResult {
        val fixMethod = pickOptimalFixMethod(previousFixVersion)

        return if (fixMethod != null) {
            fixMethod.method(itemId)
            val item = itemRepository.findById(itemId).awaitSingleOrNull()
            FixAttemptResult(
                fixVersionApplied = fixMethod.fixVersion,
                itemId = itemId,
                item = item
            )
        } else {
            FixAttemptResult(
                fixVersionApplied = null,
                itemId = itemId,
                item = null,
            )
        }
    }

    private fun pickOptimalFixMethod(previousFixVersion: Int?): FixAttemptMethod? {
        if (previousFixVersion == null) return fixAttemptMethods.first()
        return fixAttemptMethods.firstOrNull { it.fixVersion > previousFixVersion }
    }

    private suspend fun fixInternal(itemId: ItemId, deleteOwnerships: Boolean = false) {
        logger.info("Attempting to fix item<->ownership consistency for item $itemId")
        if (deleteOwnerships) {
            ownershipUpdateService.deleteByItemId(itemId = itemId)
        }
        itemReduceService.update(itemId.token, itemId.tokenId).awaitFirstOrNull()
        logger.info("Attempt finished for item $itemId")
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
            val ownerships = ownershipService.search(filter, continuation, limit)
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
        val fixVersionApplied: Int?,
        val itemId: ItemId,
        val item: Item?,
    )

    private data class FixAttemptMethod(
        val fixVersion: Int,
        val weight: Int,
        val method: suspend (ItemId) -> Unit
    )
}
