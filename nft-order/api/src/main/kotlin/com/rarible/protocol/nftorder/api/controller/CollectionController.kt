package com.rarible.protocol.nftorder.api.controller

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.dto.NftTokenIdDto
import com.rarible.protocol.nftorder.api.service.CollectionApiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectionController(
    private val collectionApiService: CollectionApiService
) : NftOrderCollectionControllerApi {

    override suspend fun generateNftOrderTokenId(collection: String, minter: String): ResponseEntity<NftTokenIdDto> {
        val result = collectionApiService.generateTokenId(collection, minter)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderCollectionById(collection: String): ResponseEntity<NftCollectionDto> {
        val result = collectionApiService.getCollectionById(collection)
        return ResponseEntity.ok(result)
    }

    override suspend fun searchNftOrderAllCollections(
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftCollectionsDto> {
        val result = collectionApiService.searchAllCollections(continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun searchNftOrderCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftCollectionsDto> {
        val result = collectionApiService.searchCollectionsByOwner(owner, continuation, size)
        return ResponseEntity.ok(result)
    }
}
