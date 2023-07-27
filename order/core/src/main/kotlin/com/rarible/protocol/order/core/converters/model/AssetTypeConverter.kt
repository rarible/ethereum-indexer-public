package com.rarible.protocol.order.core.converters.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AmmNftAssetTypeDto
import com.rarible.protocol.dto.AssetTypeDto
import com.rarible.protocol.dto.CollectionAssetTypeDto
import com.rarible.protocol.dto.CryptoPunksAssetTypeDto
import com.rarible.protocol.dto.Erc1155AssetTypeDto
import com.rarible.protocol.dto.Erc1155LazyAssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.Erc721LazyAssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.GenerativeArtAssetTypeDto
import com.rarible.protocol.order.core.model.AmmNftAssetType
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc1155LazyAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Erc721LazyAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.GenerativeArtAssetType
import com.rarible.protocol.order.core.model.Part
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
                token = source.contract,
                tokenId = EthUInt256.of(source.tokenId)
            )
            is CollectionAssetTypeDto -> CollectionAssetType(
                token = source.contract
            )
            is AmmNftAssetTypeDto -> AmmNftAssetType(
                token = source.contract
            )
        }
    }
}
