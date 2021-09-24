package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object AssetTypeDtoConverter : Converter<AssetType, AssetTypeDto> {
    override fun convert(source: AssetType): AssetTypeDto {
        return when (source) {
            is EthAssetType -> EthAssetTypeDto()
            is GenerativeArtAssetType -> GenerativeArtAssetTypeDto(
                contract = source.token
            )
            is Erc20AssetType -> Erc20AssetTypeDto(
                contract = source.token
            )
            is Erc721AssetType -> Erc721AssetTypeDto(
                contract = source.token,
                tokenId = source.tokenId.value
            )
            is Erc1155AssetType -> Erc1155AssetTypeDto(
                contract = source.token,
                tokenId = source.tokenId.value
            )
            is Erc721LazyAssetType -> Erc721LazyAssetTypeDto(
                contract = source.token,
                tokenId = source.tokenId.value,
                uri = source.uri,
                creators = source.creators.map { PartDto(it.account, it.value.value.intValueExact()) },
                royalties = source.royalties.map { PartDto(it.account, it.value.value.intValueExact()) },
                signatures = source.signatures
            )
            is Erc1155LazyAssetType -> Erc1155LazyAssetTypeDto(
                contract = source.token,
                tokenId = source.tokenId.value,
                uri = source.uri,
                supply = source.supply.value,
                creators = source.creators.map { PartDto(it.account, it.value.value.intValueExact()) },
                royalties = source.royalties.map { PartDto(it.account, it.value.value.intValueExact()) },
                signatures = source.signatures
            )
            is CryptoPunksAssetType -> CryptoPunksAssetTypeDto(
                contract = source.token,
                punkId = source.tokenId.value.toInt()
            )
        }
    }
}
