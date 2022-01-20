package com.rarible.protocol.order.api.data

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.Platform
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

fun OrderVersion.withMakeToken(token: Address): OrderVersion {
    return when (val makeType = make.type) {
        is Erc721AssetType -> copy(make = make.copy(type = makeType.copy(token = token)))
        is Erc1155AssetType -> copy(make = make.copy(type = makeType.copy(token = token)))
        is CryptoPunksAssetType -> copy(make = make.copy(type = makeType.copy(token = token)))
        is CollectionAssetType -> copy(make = make.copy(type = makeType.copy(token = token)))
        else -> throw IllegalArgumentException("Unsupported make assert type ${makeType.javaClass}")
    }
}

fun OrderVersion.withTakeToken(token: Address): OrderVersion {
    return when (val takeType = take.type) {
        is Erc721AssetType -> copy(take = take.copy(type = takeType.copy(token = token)))
        is Erc1155AssetType -> copy(take = take.copy(type = takeType.copy(token = token)))
        is CryptoPunksAssetType -> copy(take = take.copy(type = takeType.copy(token = token)))
        is CollectionAssetType -> copy(take = take.copy(type = takeType.copy(token = token)))
        else -> throw IllegalArgumentException("Unsupported take assert type ${takeType.javaClass}")
    }
}

fun OrderVersion.withMakeTokenId(tokenId: EthUInt256): OrderVersion {
    return when (val makeType = make.type) {
        is Erc721AssetType -> copy(make = make.copy(type = makeType.copy(tokenId = tokenId)))
        is Erc1155AssetType -> copy(make = make.copy(type = makeType.copy(tokenId = tokenId)))
        is CryptoPunksAssetType -> copy(make = make.copy(type = makeType.copy(tokenId = tokenId)))
        else -> throw IllegalArgumentException("Unsupported make assert type ${makeType.javaClass}")
    }
}

fun OrderVersion.withMakeValue(value: EthUInt256): OrderVersion {
    return when (val makeType = make.type) {
        is Erc721AssetType -> copy(make = make.copy(value = value))
        is Erc1155AssetType -> copy(make = make.copy(value = value))
        is CryptoPunksAssetType -> copy(make = make.copy(value = value))
        else -> throw IllegalArgumentException("Unsupported make assert type ${makeType.javaClass}")
    }
}

fun OrderVersion.withCreatedAt(date: Instant): OrderVersion {
    return copy(createdAt = date)
}

fun OrderVersion.withTakeNft(token: Address, tokenId: EthUInt256): OrderVersion {
    return withTakeToken(token).withTakeTokenId(tokenId)
}

fun OrderVersion.withOrigin(origin: Address): OrderVersion {
    return when (val versionData = data) {
        is OrderRaribleV2DataV1 -> copy(data = versionData.copy(originFees = listOf(Part(origin, EthUInt256.of(10000)))))
        is OrderRaribleV2DataV2 -> copy(data = versionData.copy(originFees = listOf(Part(origin, EthUInt256.of(10000)))))
        else -> throw IllegalArgumentException("Unsupported data type ${data.javaClass} to set origin")
    }
}

fun OrderVersion.withMakeNft(token: Address, tokenId: EthUInt256): OrderVersion {
    return withMakeToken(token).withMakeTokenId(tokenId)
}

fun OrderVersion.withTakePriceUsd(takePriceUsd: BigDecimal): OrderVersion {
    return copy(takePriceUsd = takePriceUsd)
}

fun OrderVersion.withTakeTokenId(tokenId: EthUInt256): OrderVersion {
    return when (val takeType = take.type) {
        is Erc721AssetType -> copy(take = take.copy(type = takeType.copy(tokenId = tokenId)))
        is Erc1155AssetType -> copy(take = take.copy(type = takeType.copy(tokenId = tokenId)))
        is CryptoPunksAssetType -> copy(take = take.copy(type = takeType.copy(tokenId = tokenId)))
        else -> throw IllegalArgumentException("Unsupported take assert type ${takeType.javaClass}")
    }
}

fun createErc721BidOrderVersion(): OrderVersion {
    val make = createEthAsset()
    val take = createErc721Asset()
    return createOrderVersion(make, take)
}

fun createErc1155BidOrderVersion(): OrderVersion {
    val make = createEthAsset()
    val take = createErc1155Asset()
    return createOrderVersion(make, take)
}

fun createErc721ListOrderVersion(): OrderVersion {
    val make = createErc721Asset()
    val take = createEthAsset()
    return createOrderVersion(make, take)
}

fun createCollectionOrderVersion(): OrderVersion {
    val make = createCollectionAsset()
    val take = createEthAsset()
    return createOrderVersion(make, take)
}

fun createCollectionBidOrderVersion(): OrderVersion {
    val make = createEthAsset()
    val take = createCollectionAsset()
    return createOrderVersion(make, take)
}

fun createErc1155ListOrderVersion(): OrderVersion {
    val make = createErc1155Asset()
    val take = createEthAsset()
    return createOrderVersion(make, take)
}

fun createOrderVersion(make: Asset, take: Asset) = OrderVersion(
    hash = Word.apply(RandomUtils.nextBytes(32)),
    maker = randomAddress(),
    taker = randomAddress(),
    makePriceUsd = (1..100).random().toBigDecimal(),
    takePriceUsd = (1..100).random().toBigDecimal(),
    makePrice = (1..100).random().toBigDecimal(),
    takePrice = (1..100).random().toBigDecimal(),
    makeUsd = (1..100).random().toBigDecimal(),
    takeUsd = (1..100).random().toBigDecimal(),
    make = make,
    take = take,
    platform = Platform.RARIBLE,
    type = OrderType.RARIBLE_V2,
    salt = EthUInt256.TEN,
    start = null,
    end = null,
    data = OrderRaribleV2DataV1(emptyList(), emptyList()),
    signature = null
)
