package com.rarible.protocol.order.core.converters.model

import com.rarible.ethereum.nft.model.LazyERC1155
import com.rarible.ethereum.nft.model.LazyERC721
import com.rarible.ethereum.nft.model.LazyNft
import com.rarible.protocol.order.core.model.*
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import com.rarible.ethereum.nft.model.Part as LazyNftRoyaltiesPart

@Component
object LazyAssetTypeToLazyNftConverter : Converter<AssetType, LazyNft> {
    override fun convert(source: AssetType): LazyNft {
        return when (source) {
            is Erc721LazyAssetType -> {
                LazyERC721(
                    token = source.token,
                    tokenId = source.tokenId.value,
                    uri = source.uri,
                    creators = source.creators.map { convert(it) },
                    signatures = source.signatures,
                    royalties = source.royalties.map { convert(it) }
                )
            }
            is Erc1155LazyAssetType -> {
                LazyERC1155(
                    token = source.token,
                    tokenId = source.tokenId.value,
                    supply = source.supply.value,
                    uri = source.uri,
                    creators = source.creators.map { convert(it) },
                    signatures = source.signatures,
                    royalties = source.royalties.map { convert(it) }
                )
            }
            is Erc1155AssetType, is Erc20AssetType, is Erc721AssetType, is CryptoPunksAssetType, is GenerativeArtAssetType, EthAssetType -> {
                throw IllegalArgumentException("Assert $source is not lazy to convert to lazy nft")
            }
        }
    }

    private fun convert(source: Part): LazyNftRoyaltiesPart {
        return LazyNftRoyaltiesPart(source.account, source.value.value.toInt())
    }
}
