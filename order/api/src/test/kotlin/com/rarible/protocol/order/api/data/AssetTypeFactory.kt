package com.rarible.protocol.order.api.data

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Binary
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.util.*

fun createErc721AssetType() = Erc721AssetType(
    token = AddressFactory.create(),
    tokenId = EthUInt256.of((1L..100L).random())
)

fun createErc1155AssetType() = Erc1155AssetType(
    token = AddressFactory.create(),
    tokenId = EthUInt256.of((1L..100L).random())
)

fun createErc721LazyAssetType(): Erc721LazyAssetType {
    return Erc721LazyAssetType(
        token = AddressFactory.create(),
        tokenId = EthUInt256.of((1L..100L).random()),
        uri = UUID.randomUUID().toString(),
        creators = creators((1L..100L).map { AddressFactory.create() }),
        signatures = (1L..100L).map { Binary.apply(ByteArray(65)) },
        royalties = emptyList()
    )
}

private fun creators(creators: List<Address>): List<Part> {
    val every = 10000L / creators.size
    return creators.map { Part(it, EthUInt256.of(every)) }
}

fun createErc1155LazyAssetType(): Erc1155LazyAssetType {
    return Erc1155LazyAssetType(
        token = AddressFactory.create(),
        tokenId = EthUInt256.of((1L..100L).random()),
        supply = EthUInt256.of((1L..100L).random()),
        uri = UUID.randomUUID().toString(),
        creators = creators((1L..100L).map { AddressFactory.create() }),
        signatures = (1L..100L).map { Binary.apply(ByteArray(65)) },
        royalties = emptyList()
    )
}

fun createCollectionAssetType() = CollectionAssetType(
    token = AddressFactory.create()
)
