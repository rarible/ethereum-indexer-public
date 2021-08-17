package com.rarible.protocol.order.core.converters.model


import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import scalether.domain.Address


@Component
object AssetTypeConverter : Converter<AssetTypeDto, AssetType> {
    override fun convert(source: AssetTypeDto): AssetType {
        return when (source) {
            is EthAssetTypeDto -> EthAssetType
            is GenerativeArtAssetTypeDto -> GenerativeArtAssetType(
                token = source.contract
            )
            is Erc20AssetTypeDto -> Erc20AssetType(Address.apply(source.contract))
            is Erc721AssetTypeDto ->
                Erc721AssetType(Address.apply(source.contract), EthUInt256(source.tokenId))
            is Erc1155AssetTypeDto ->
                Erc1155AssetType(Address.apply(source.contract), EthUInt256(source.tokenId))
            is Erc721LazyAssetTypeDto -> Erc721LazyAssetType(
                token = source.contract,
                tokenId = EthUInt256(source.tokenId),
                uri = source.uri,
                creators = source.creators.map { Part(it.account, EthUInt256.of(it.value.toLong())) },
                royalties = source.royalties.map { Part(it.account, EthUInt256.of(it.value.toLong())) },
                signatures = source.signatures
            )
            is Erc1155LazyAssetTypeDto -> Erc1155LazyAssetType(
                token = source.contract,
                tokenId = EthUInt256(source.tokenId),
                uri = source.uri,
                supply = EthUInt256.of(source.supply),
                creators = source.creators.map { Part(it.account, EthUInt256.of(it.value.toLong())) },
                royalties = source.royalties.map { Part(it.account, EthUInt256.of(it.value.toLong())) },
                signatures = source.signatures
            )
            is CryptoPunksAssetTypeDto -> CryptoPunksAssetType(
                marketAddress = source.contract,
                punkId = source.punkId
            )
            is FlowAssetTypeDto -> throw IllegalArgumentException("Unsupported assert type ${source.javaClass} ")
        }
    }
}
