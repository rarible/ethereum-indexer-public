package com.rarible.protocol.order.core.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger

fun createOrder() =
    Order(
        maker = AddressFactory.create(),
        taker = null,
        make = randomErc20(EthUInt256.TEN),
        take = randomErc20(EthUInt256.of(5)),
        makeStock = EthUInt256.TEN,
        type = OrderType.RARIBLE_V2,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis()
    )

fun createOrderVersion(): OrderVersion {
    return OrderVersion(
        maker = randomAddress(),
        taker = null,
        make = randomErc20(EthUInt256.TEN),
        take = randomErc20(EthUInt256.of(5)),
        createdAt = nowMillis(),
        makePriceUsd = null,
        takePriceUsd = null,
        makePrice = null,
        takePrice = null,
        makeUsd = null,
        takeUsd = null,
        platform = Platform.RARIBLE,
        type = OrderType.RARIBLE_V2,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null
    )
}

fun createOrderOpenSeaV1DataV1(): OrderOpenSeaV1DataV1 {
    return OrderOpenSeaV1DataV1(
        exchange = randomAddress(),
        makerRelayerFee = BigInteger.ZERO,
        takerRelayerFee = BigInteger.ZERO,
        makerProtocolFee = BigInteger.ZERO,
        takerProtocolFee = BigInteger.ZERO,
        feeRecipient = randomAddress(),
        feeMethod = OpenSeaOrderFeeMethod.values().random(),
        side = OpenSeaOrderSide.values().random(),
        saleKind = OpenSeaOrderSaleKind.values().random(),
        howToCall = OpenSeaOrderHowToCall.values().random(),
        callData = Binary.empty(),
        replacementPattern = Binary.empty(),
        staticTarget = randomAddress(),
        staticExtraData = Binary.empty(),
        extra = BigInteger.ZERO,
        target = randomAddress(),
        nonce = randomLong()
    )
}

fun Order.withMakeFill(isMakeFill: Boolean = true): Order {
    val newData = data.withMakeFill(isMakeFill)
    return copy(
        data = newData,
        hash = Order.hashKey(maker, make.type, take.type, salt.value, newData)
    )
}

fun OrderData.withMakeFill(isMakeFill: Boolean = true): OrderData = when (this) {
    is OrderRaribleV2DataV1 -> OrderRaribleV2DataV2(
        payouts = payouts,
        originFees = originFees,
        isMakeFill = isMakeFill
    )
    is OrderRaribleV2DataV2 -> this.copy(isMakeFill = isMakeFill)
    else -> this
}

fun createOrderDto() =
    RaribleV2OrderDto(
        maker = AddressFactory.create(),
        taker = null,
        make = AssetDto(Erc20AssetTypeDto(AddressFactory.create()), BigInteger.TEN, BigDecimal.TEN),
        take = AssetDto(Erc20AssetTypeDto(AddressFactory.create()), BigInteger.TEN, BigDecimal.TEN),
        fill = BigInteger.ZERO,
        fillValue = BigDecimal.ZERO,
        makeStock = BigInteger.TEN,
        makeStockValue = BigDecimal.TEN,
        cancelled = false,
        salt = Word.apply(ByteArray(32)),
        data = OrderRaribleV2DataV1Dto(emptyList(), emptyList()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = emptyList(),
        hash = WordFactory.create(),
        makeBalance = BigInteger.TEN,
        makePriceUsd = null,
        takePriceUsd = null,
        start = null,
        end = null,
        priceHistory = listOf()
    )

fun createOrderCancel(): OrderCancel {
    return OrderCancel(
        hash = WordFactory.create(),
        maker = AddressFactory.create(),
        make = randomErc20(EthUInt256.TEN),
        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))
    )
}

fun createOrderSideMatch(): OrderSideMatch {
    return OrderSideMatch(
        hash = WordFactory.create(),
        maker = AddressFactory.create(),
        taker = AddressFactory.create(),
        make = randomErc20(EthUInt256.TEN),
        take = randomErc20(EthUInt256.of(5)),
        fill = EthUInt256.ZERO,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        side = OrderSide.LEFT,
        makeUsd = null,
        takeUsd = null,
        makePriceUsd = null,
        takePriceUsd = null,
        makeValue = null,
        takeValue = null
    )
}

fun randomErc20(value: EthUInt256) = Asset(Erc20AssetType(AddressFactory.create()), value)

fun randomErc1155(value: EthUInt256) = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256(randomBigInt())), value)

fun randomErc721() = Asset(Erc721AssetType(AddressFactory.create(), EthUInt256(randomBigInt())), EthUInt256.ONE)

fun randomPart() = Part(randomAddress(), EthUInt256(randomBigInt()))

fun randomPartDto() = PartDto(randomAddress(), randomInt())

fun randomErc1155LazyAssetType() =
    Erc1155LazyAssetType(
        token = AddressFactory.create(),
        tokenId = EthUInt256(randomBigInt()),
        uri = randomString(),
        creators = emptyList(),
        royalties = emptyList(),
        signatures = emptyList(),
        supply = EthUInt256.TEN
    )

fun randomErc721LazyAssetType() =
    Erc721LazyAssetType(
        token = AddressFactory.create(),
        tokenId = EthUInt256(randomBigInt()),
        uri = randomString(),
        creators = emptyList(),
        royalties = emptyList(),
        signatures = emptyList()
    )
