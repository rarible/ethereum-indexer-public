package com.rarible.protocol.nftorder.api.service

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.dto.NftTokenIdDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Complete proxy for NftCollectionController
 */
@Component
class CollectionApiService(
    private val nftCollectionControllerApi: NftCollectionControllerApi
) {

    private val logger = LoggerFactory.getLogger(CollectionApiService::class.java)

    suspend fun generateTokenId(collection: String, minter: String): NftTokenIdDto {
        logger.debug("Generating Collection token id: collection=[{}], minter=[{}]", collection, minter)
        return nftCollectionControllerApi.generateNftTokenId(collection, minter).awaitFirst()
    }

    suspend fun getCollectionById(collection: String): NftCollectionDto {
        logger.debug("Get Collection by id: collection=[{}]", collection)
        return nftCollectionControllerApi.getNftCollectionById(collection).awaitFirst()
    }

    suspend fun searchAllCollections(continuation: String?, size: Int?): NftCollectionsDto {
        logger.debug("Search all Collections with params: continuation={}, size={}", continuation, size)
        return nftCollectionControllerApi.searchNftAllCollections(continuation, size).awaitFirst()
    }

    suspend fun searchCollectionsByOwner(owner: String, continuation: String?, size: Int?): NftCollectionsDto {
        logger.debug(
            "Search all Collections with params: owner=[{}], continuation={}, size={}",
            owner, continuation, size
        )
        return nftCollectionControllerApi.searchNftCollectionsByOwner(owner, continuation, size).awaitFirst()
    }


}