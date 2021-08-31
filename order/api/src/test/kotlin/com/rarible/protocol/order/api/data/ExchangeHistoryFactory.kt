package com.rarible.protocol.order.api.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

fun OrderSideMatch.withMakeToken(token: Address): OrderSideMatch {
    return when (val makeType = make.type) {
        is Erc721AssetType -> copy(make = make.copy(type = makeType.copy(token = token)))
        is Erc1155AssetType -> copy(make = make.copy(type = makeType.copy(token = token)))
        else -> throw IllegalArgumentException("Unsupported make assert type ${makeType.javaClass}")
    }
}

fun OrderCancel.withMakeToken(token: Address): OrderCancel {
    return when (val makeType = make?.type) {
        is Erc721AssetType -> copy(make = make?.copy(type = makeType.copy(token = token)))
        is Erc1155AssetType -> copy(make = make?.copy(type = makeType.copy(token = token)))
        else -> throw IllegalArgumentException("Unsupported make assert type ${makeType?.javaClass}")
    }
}

fun OrderCancel.withTakeToken(token: Address): OrderCancel {
    return when (val takeType = take?.type) {
        is Erc721AssetType -> copy(take = take?.copy(type = takeType.copy(token = token)))
        is Erc1155AssetType -> copy(take = take?.copy(type = takeType.copy(token = token)))
        else -> throw IllegalArgumentException("Unsupported take assert type ${takeType?.javaClass}")
    }
}

fun OrderSideMatch.withMaker(maker: Address): OrderSideMatch {
    return when (val makeType = make.type) {
        is Erc721AssetType -> copy(maker = maker)
        is Erc1155AssetType -> copy(maker = maker)
        else -> throw IllegalArgumentException("Unsupported make assert type ${makeType.javaClass}")
    }
}

fun OrderSideMatch.withTaker(taker: Address): OrderSideMatch {
    return copy(taker = taker)
}

fun OrderSideMatch.withTakeUsd(value: BigDecimal): OrderSideMatch {
    return when (val makeType = make.type) {
        is Erc721AssetType -> copy(takeUsd = value)
        is Erc1155AssetType -> copy(takeUsd = value)
        else -> throw IllegalArgumentException("Unsupported make assert type ${makeType.javaClass}")
    }
}

fun OrderSideMatch.withMakeTokenId(tokenId: EthUInt256): OrderSideMatch {
    return when (val makeType = make.type) {
        is Erc721AssetType -> copy(make = make.copy(type = makeType.copy(tokenId = tokenId)))
        is Erc1155AssetType -> copy(make = make.copy(type = makeType.copy(tokenId = tokenId)))
        else -> throw IllegalArgumentException("Unsupported make assert type ${makeType.javaClass}")
    }
}

fun OrderCancel.withMakeTokenId(tokenId: EthUInt256): OrderCancel {
    return when (val makeType = make?.type) {
        is Erc721AssetType -> copy(make = make?.copy(type = makeType.copy(tokenId = tokenId)))
        is Erc1155AssetType -> copy(make = make?.copy(type = makeType.copy(tokenId = tokenId)))
        else -> throw IllegalArgumentException("Unsupported make assert type ${makeType?.javaClass}")
    }
}

fun OrderCancel.withTakeTokenId(tokenId: EthUInt256): OrderCancel {
    return when (val takeType = take?.type) {
        is Erc721AssetType -> copy(take = take?.copy(type = takeType.copy(tokenId = tokenId)))
        is Erc1155AssetType -> copy(take = take?.copy(type = takeType.copy(tokenId = tokenId)))
        else -> throw IllegalArgumentException("Unsupported make assert type ${take?.javaClass}")
    }
}

fun OrderSideMatch.withDate(date: Instant): OrderSideMatch {
    return copy(date = date)
}

fun OrderSideMatch.withMakeNft(token: Address, tokenId: EthUInt256): OrderSideMatch {
    return withMakeToken(token).withMakeTokenId(tokenId)
}

fun OrderCancel.withMakeNft(token: Address, tokenId: EthUInt256): OrderCancel {
    return withMakeToken(token).withMakeTokenId(tokenId)
}

fun OrderCancel.withTakeNft(token: Address, tokenId: EthUInt256): OrderCancel {
    return withTakeToken(token).withTakeTokenId(tokenId)
}

fun OrderCancel.withMaker(maker: Address): OrderCancel {
    return copy(maker = maker)
}

fun OrderCancel.withDate(date: Instant): OrderCancel {
    return copy(date = date)
}

fun orderErc721SellSideMatch(): OrderSideMatch {
    val make = createErc721Asset()
    val take = createEthAsset()
    return orderSideMatch(make, take)
}

fun orderErc1155SellSideMatch(): OrderSideMatch {
    val make = createErc1155Asset()
    val take = createEthAsset()
    return orderSideMatch(make, take)
}

fun orderErc1155SellCancel(): OrderCancel {
    val make = createErc1155Asset()
    val take = createEthAsset()
    return orderCancel(make, take)
}

fun orderErc1155BidCancel(): OrderCancel {
    val make = createEthAsset()
    val take = createErc1155Asset()
    return orderCancel(make, take)
}

fun orderErc721SellCancel(): OrderCancel {
    val make = createErc721Asset()
    val take = createEthAsset()
    return orderCancel(make, take)
}

fun orderErc721BidCancel(): OrderCancel {
    val make = createEthAsset()
    val take = createErc721Asset()
    return orderCancel(make, take)
}

fun createLogEvent(data: OrderExchangeHistory) = LogEvent(
    data = data,
    address = createAddress(),
    topic = Word.apply(RandomUtils.nextBytes(32)),
    transactionHash = Word.apply(RandomUtils.nextBytes(32)),
    index = RandomUtils.nextInt(),
    minorLogIndex = 0,
    status = LogEventStatus.CONFIRMED
)

fun orderSideMatch(make: Asset, take: Asset): OrderSideMatch = OrderSideMatch(
    hash = Word.apply(RandomUtils.nextBytes(32)),
    counterHash = Word.apply(RandomUtils.nextBytes(32)),
    side = OrderSide.LEFT,
    fill = EthUInt256.ONE,
    make = make,
    take = take,
    maker = createAddress(),
    taker = createAddress(),
    makeUsd = null,
    takeUsd = null,
    makePriceUsd = null,
    takePriceUsd = null,
    makeValue = null,
    takeValue = null,
    source = HistorySource.RARIBLE
)

fun orderCancel(make: Asset, take: Asset): OrderCancel = OrderCancel(
    hash = Word.apply(RandomUtils.nextBytes(32)),
    maker = createAddress(),
    make = make,
    take = take,
    date = nowMillis(),
    source = HistorySource.RARIBLE
)
