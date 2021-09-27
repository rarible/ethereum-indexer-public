package com.rarible.protocol.nftorder.api.service

import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftOrderItemDto
import com.rarible.protocol.nft.api.client.NftLazyMintControllerApi
import com.rarible.protocol.nftorder.core.converter.NftItemDtoToNftOrderItemDtoConverter
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LazyMintApiService(
    private val nftLazyMintControllerApi: NftLazyMintControllerApi
) {

    private val logger = LoggerFactory.getLogger(LazyMintApiService::class.java)

    suspend fun mintAsset(lazyNftDto: LazyNftDto): NftOrderItemDto {
        logger.debug(
            "Lazy mint with NFT: contract=[{}], token=[{}], uri=[{}]",
            lazyNftDto.contract, lazyNftDto.tokenId, lazyNftDto.uri
        )
        val nftItem = nftLazyMintControllerApi.mintNftAsset(lazyNftDto).awaitFirst()
        // TODO should be add here enrich data? I guess we should not
        return NftItemDtoToNftOrderItemDtoConverter.convert(nftItem)
    }
}
