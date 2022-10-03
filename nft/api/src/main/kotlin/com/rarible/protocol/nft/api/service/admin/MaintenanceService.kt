package com.rarible.protocol.nft.api.service.admin

import com.rarible.protocol.nft.api.dto.FixUserItemsResultDto
import com.rarible.protocol.nft.api.service.item.ItemService
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService.CheckResult.Failure
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Service
import java.util.function.Consumer
import java.util.function.Supplier

@Service
class MaintenanceService(
    private val ownershipApiService: OwnershipApiService,
    private val itemService: ItemService,
    private val itemReduceService: ItemReduceService,
    private val itemOwnershipConsistencyService: ItemOwnershipConsistencyService,
) {

    suspend fun fixUserItems(owner: String): FixUserItemsResultDto {
        val resultValid = mutableSetOf<ItemId>()
        val resultFixed = mutableSetOf<ItemId>()
        val resultUnfixed = mutableSetOf<ItemId>()

        val ownerships = ownershipApiService.getAllByOwner(owner)
        val itemIds = ownerships.map { ItemId(it.token, it.tokenId) }.toSet()
        val itemIdsToFix = mutableSetOf<ItemId>()

        validateItemIds(itemIds, resultValid::add, itemIdsToFix::add)

        itemIdsToFix.forEach { itemId ->
            itemReduceService.update(itemId.token, itemId.tokenId).then().awaitFirstOrNull()
        }

        validateItemIds(itemIdsToFix, resultFixed::add, resultUnfixed::add)

        return FixUserItemsResultDto(
            resultValid.map { it.toString() }.toList(),
            resultFixed.map { it.toString() }.toList(),
            resultUnfixed.map { it.toString() }.toList(),
        )
    }

    private suspend fun validateItemIds(
        itemIds: Set<ItemId>,
        validConsumer: Consumer<ItemId>,
        invalidConsumer: Consumer<ItemId>,
    ) {
        val foundItems = itemService.getAll(itemIds)
        val foundItemIds = foundItems.map { it.id }.toSet()

        foundItems.forEach { item ->
            if (itemOwnershipConsistencyService.checkItem(item) is Failure) {
                invalidConsumer.accept(item.id)
            } else {
                validConsumer.accept(item.id)
            }
        }

        itemIds.forEach { itemId ->
            if (!foundItemIds.contains(itemId)) {
                invalidConsumer.accept(itemId)
            }
        }
    }
}
