package com.rarible.protocol.nft.api.service.admin

import com.rarible.protocol.nft.api.model.ItemProblemType
import com.rarible.protocol.nft.api.model.ItemResult
import com.rarible.protocol.nft.api.model.ItemStatus
import com.rarible.protocol.nft.api.service.item.ItemService
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService.CheckResult.Failure
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MaintenanceService(
    private val ownershipApiService: OwnershipApiService,
    private val itemService: ItemService,
    private val itemReduceService: ItemReduceService,
    private val itemOwnershipConsistencyService: ItemOwnershipConsistencyService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun fixUserItems(owner: String): List<ItemResult> {
        val result = mutableListOf<ItemResult>()

        val resultBeforeFix = checkUserItems(owner)
        val itemIdsToFix = mutableMapOf<ItemId, ItemProblemType?>()

        resultBeforeFix.forEach { itemCheckResult ->
            if (itemCheckResult.status == ItemStatus.INVALID) {
                val itemId = itemCheckResult.itemId
                logger.info("Attempting to fix itemId $itemId of owner $owner")
                itemReduceService.update(itemId.token, itemId.tokenId).then().awaitFirstOrNull()
                itemIdsToFix[itemId] = itemCheckResult.problem
            } else {
                result.add(itemCheckResult)
            }
        }

        validateItemIds(itemIdsToFix.keys).forEach { itemCheckResult ->
            if (itemCheckResult.status == ItemStatus.VALID) {
                result.add(
                    ItemResult(
                        itemId = itemCheckResult.itemId,
                        status = ItemStatus.FIXED,
                        problem = itemIdsToFix[itemCheckResult.itemId]
                    )
                )
            } else {
                result.add(
                    ItemResult(
                        itemId = itemCheckResult.itemId,
                        status = ItemStatus.UNFIXED,
                        problem = itemIdsToFix[itemCheckResult.itemId]
                    )
                )
            }
        }

        return result
    }

    suspend fun checkUserItems(owner: String): List<ItemResult> {
        val ownerships = ownershipApiService.getAllByOwner(owner)
        logger.info("Got ${ownerships.size} ownerships of owner $owner")
        val itemIds = ownerships.map { ItemId(it.token, it.tokenId) }.toSet()

        return validateItemIds(itemIds)
    }

    private suspend fun validateItemIds(
        itemIds: Set<ItemId>,
    ): List<ItemResult> {
        val result = mutableListOf<ItemResult>()
        val foundItems = itemService.getAll(itemIds)
        val foundItemIds = foundItems.map { it.id }.toSet()

        foundItems.forEach { item ->
            if (itemOwnershipConsistencyService.checkItem(item) is Failure) {
                result.add(ItemResult(item.id, ItemStatus.INVALID, ItemProblemType.SUPPLY_MISMATCH))
            } else {
                result.add(ItemResult(item.id, ItemStatus.VALID))
            }
        }

        itemIds.forEach { itemId ->
            if (!foundItemIds.contains(itemId)) {
                result.add(ItemResult(itemId, ItemStatus.INVALID, ItemProblemType.NOT_FOUND))
            }
        }

        return result
    }
}
