package com.rarible.protocol.nftorder.api.controller

import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftOrderItemDto
import com.rarible.protocol.dto.NftOrderItemsPageDto
import com.rarible.protocol.nftorder.api.service.ItemApiService
import com.rarible.protocol.nftorder.core.model.ItemId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ItemController(
    private val itemApiService: ItemApiService
) : NftOrderItemControllerApi {

    override suspend fun getNftOrderItemById(itemId: String): ResponseEntity<NftOrderItemDto> {
        val result = itemApiService.getItemById(ItemId.parseId(itemId))
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderAllItems(
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ResponseEntity<NftOrderItemsPageDto> {
        val result = itemApiService.getAllItems(continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftOrderItemsPageDto> {
        val result = itemApiService.getItemsByOwner(owner, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftOrderItemsPageDto> {
        val result = itemApiService.getItemsByCreator(creator, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftOrderItemsPageDto> {
        val result = itemApiService.getItemsByCollection(collection, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderLazyItemById(itemId: String): ResponseEntity<LazyNftDto> {
        val result = itemApiService.getLazyItemById(itemId)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderItemMetaById(itemId: String): ResponseEntity<NftItemMetaDto> {
        val result = itemApiService.getItemMetaById(itemId)
        return ResponseEntity.ok(result)
    }
}
