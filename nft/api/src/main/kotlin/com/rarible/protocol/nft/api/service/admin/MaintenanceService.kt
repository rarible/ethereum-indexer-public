package com.rarible.protocol.nft.api.service.admin

import com.rarible.protocol.nft.api.dto.FixUserItemsResultDto
import com.rarible.protocol.nft.api.service.item.ItemService
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Service

@Service
class MaintenanceService(
    private val ownershipApiService: OwnershipApiService,
    private val itemService: ItemService,
    private val itemReduceService: ItemReduceService,
) {

    suspend fun fixUserItems(owner: String): FixUserItemsResultDto {
        val resultValid = mutableListOf<String>()
        val resultFixed = mutableListOf<String>()
        val resultUnfixed = mutableListOf<String>()

        val ownerships = ownershipApiService.getAllByOwner(owner)
        val itemIds = ownerships.map { ItemId(it.token, it.tokenId) }.toSet()
        val foundItemIds = itemService.getAll(itemIds).map { it.id }.toSet()

        val checkFixed = mutableSetOf<ItemId>()
        itemIds.forEach { itemId ->
            if (foundItemIds.contains(itemId)) {
                resultValid.add(itemId.toString())
            } else {
                itemReduceService.update(itemId.token, itemId.tokenId).then().awaitFirstOrNull()
                checkFixed.add(itemId)
            }
        }

        val fixedItemIds = itemService.getAll(checkFixed).map { it.id }.toSet()
        checkFixed.forEach { itemId ->
            if (fixedItemIds.contains(itemId)) {
                resultFixed.add(itemId.toString())
            } else {
                resultUnfixed.add(itemId.toString())
            }
        }

        return FixUserItemsResultDto(resultValid, resultFixed, resultUnfixed)
    }
}
