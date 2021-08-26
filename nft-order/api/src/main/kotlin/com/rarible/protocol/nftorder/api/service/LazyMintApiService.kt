package com.rarible.protocol.nftorder.api.service

import com.rarible.core.common.convert
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftOrderItemDto
import com.rarible.protocol.nft.api.client.NftLazyMintControllerApi
import com.rarible.protocol.nftorder.core.model.Item
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class LazyMintApiService(
    private val nftLazyMintControllerApi: NftLazyMintControllerApi,
    private val conversionService: ConversionService
) {

    private val logger = LoggerFactory.getLogger(LazyMintApiService::class.java)

    suspend fun mintAsset(lazyNftDto: LazyNftDto): NftOrderItemDto {
        logger.debug(
            "Lazy mint with NFT: contract=[{}], token=[{}], uri=[{}]",
            lazyNftDto.contract, lazyNftDto.tokenId, lazyNftDto.uri
        )
        val nftItem = nftLazyMintControllerApi.mintNftAsset(lazyNftDto).awaitFirst()
        // TODO should be add here enrich data? I guess we should not
        val rawItem = conversionService.convert<Item>(nftItem)
        return conversionService.convert(rawItem)
    }
}