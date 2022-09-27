package com.rarible.protocol.order.core.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

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

fun createSellOrder(
    data: OrderData = createOrderRaribleV2DataV1()
) = createOrder().copy(make = randomErc721(), take = randomErc20(), data = data)

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

fun createOrderBasicSeaportDataV1(): OrderBasicSeaportDataV1 {
    return OrderBasicSeaportDataV1(
        protocol = randomAddress(),
        orderType = SeaportOrderType.values().random(),
        offer = emptyList(),
        consideration = emptyList(),
        zone = randomAddress(),
        zoneHash = Word.apply(randomWord()),
        conduitKey = Word.apply(randomWord()),
        counter = randomLong()
    )
}

fun createOrderRaribleV2DataV1(): OrderRaribleV2DataV1 {
    return OrderRaribleV2DataV1(
        originFees = listOf(Part(randomAddress(), EthUInt256.TEN), Part(randomAddress(), EthUInt256.ONE)),
        payouts = listOf(Part(randomAddress(), EthUInt256.ONE), Part(randomAddress(), EthUInt256.TEN))
    )
}

fun createOrderRaribleV1DataV3Sell(): OrderRaribleV2DataV3Sell {
    return OrderRaribleV2DataV3Sell(
        payout = cratePart(),
        originFeeFirst = cratePart(),
        originFeeSecond = cratePart(),
        maxFeesBasePoint = EthUInt256.of(randomInt()),
        marketplaceMarker = Word.apply(randomWord())
    )
}

fun createOrderRaribleV1DataV3SellDto(): OrderRaribleV2DataV3SellDto {
    return OrderRaribleV2DataV3SellDto(
        payout = cratePartDto(),
        originFeeFirst = cratePartDto(),
        originFeeSecond = cratePartDto(),
        maxFeesBasePoint = randomInt(),
        marketplaceMarker = Word.apply(randomWord())
    )
}

fun createOrderRaribleV1DataV3Buy(): OrderRaribleV2DataV3Buy {
    return OrderRaribleV2DataV3Buy(
        payout = cratePart(),
        originFeeFirst = cratePart(),
        originFeeSecond = cratePart(),
        marketplaceMarker = Word.apply(randomWord())
    )
}

fun createOrderRaribleV1DataV3BuyDto(): OrderRaribleV2DataV3BuyDto {
    return OrderRaribleV2DataV3BuyDto(
        payout = cratePartDto(),
        originFeeFirst = cratePartDto(),
        originFeeSecond = cratePartDto(),
        marketplaceMarker = Word.apply(randomWord())
    )
}

fun createOrderDataLegacy(): OrderDataLegacy {
    return OrderDataLegacy(
        fee = randomInt()
    )
}

fun createOrderX2Y2DataV1() = OrderX2Y2DataV1(
    itemHash = Word.apply(randomWord()),
    isCollectionOffer = randomBoolean(),
    isBundle = randomBoolean(),
    side = randomInt(),
    orderId = randomBigInt()
)

fun createSudoSwapPoolDataV1(
    poolAddress: Address = randomAddress()
) = SudoSwapPoolDataV1(
    poolAddress = poolAddress,
    bondingCurve = randomAddress(),
    factory = randomAddress(),
    curveType = SudoSwapCurveType.values().random(),
    assetRecipient = randomAddress(),
    poolType = SudoSwapPoolType.values().random(),
    spotPrice = randomBigInt(),
    delta = randomBigInt(),
    fee = randomBigInt(),
)

fun createOrderSudoSwapAmmDataV1(
    poolAddress: Address = randomAddress()
) = OrderSudoSwapAmmDataV1(
    poolAddress = poolAddress,
    bondingCurve = randomAddress(),
    factory = randomAddress(),
    curveType = SudoSwapCurveType.values().random(),
    assetRecipient = randomAddress(),
    poolType = SudoSwapPoolType.values().random(),
    spotPrice = randomBigInt(),
    delta = randomBigInt(),
    fee = randomBigInt(),
)

fun createSudoSwapBuyInfo(): SudoSwapBuyInfo {
    return SudoSwapBuyInfo(
        newSpotPrice = randomBigInt(),
        newDelta = randomBigInt(),
        inputValue = randomBigInt(),
        protocolFee = randomBigInt()
    )
}

fun createSudoSwapSellInfo(): SudoSwapSellInfo {
    return SudoSwapSellInfo(
        newSpotPrice = randomBigInt(),
        newDelta = randomBigInt(),
        outputValue = randomBigInt(),
        protocolFee = randomBigInt()
    )
}

fun createOrderLooksrareDataV1() = OrderLooksrareDataV1(
    minPercentageToAsk = randomInt(),
    strategy = randomAddress(),
    counter = randomLong(),
    params = randomBinary()
)

fun Order.withMakeFill(isMakeFill: Boolean = true): Order {
    val newData = data.withMakeFill(isMakeFill)
    return copy(
        data = newData,
        hash = Order.hashKey(maker, make.type, take.type, salt.value, newData)
    )
}

fun cratePart(): Part {
    return Part(randomAddress(), EthUInt256.of(randomInt()))
}

fun cratePartDto(): PartDto {
    return PartDto(randomAddress(), randomInt())
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

fun createX2Y2OrderDto(): X2Y2OrderDto {
    return X2Y2OrderDto(
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
        data = createX2Y2OrderData(),
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
}

fun createX2Y2OrderData(): OrderX2Y2DataDto {
    return OrderX2Y2DataDto(
        isBundle = false,
        isCollectionOffer = false,
        itemHash = Word.apply(randomWord()),
        orderId = randomBigInt(),
        side = randomInt()
    )
}

fun createLooksrareOrderDto(): LooksRareOrderDto {
    return LooksRareOrderDto(
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
        data = createLooksrareOrderData(),
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
}

fun createLooksrareOrderData(): OrderLooksRareDataV1Dto {
    return OrderLooksRareDataV1Dto(
        minPercentageToAsk = randomInt(),
        strategy = randomAddress(),
        nonce = randomLong(),
        params = randomBinary()
    )
}

fun createSeaportOrderDto(): SeaportV1OrderDto {
    return SeaportV1OrderDto(
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
        data = createSeaportOrderDataDto(),
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
}

fun createSeaportOrderDataDto(): OrderBasicSeaportDataV1Dto {
    return OrderBasicSeaportDataV1Dto(
        protocol = randomAddress(),
        orderType = SeaportOrderTypeDto.values().random(),
        offer = emptyList(),
        consideration = emptyList(),
        zone = randomAddress(),
        zoneHash = Word.apply(randomWord()),
        conduitKey = Word.apply(randomWord()),
        counter = randomLong()
    )
}

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

fun createLogEvent(
    data: EventData,
    status: LogEventStatus = LogEventStatus.CONFIRMED,
): LogEvent {
    return LogEvent(
        data = data,
        address = randomAddress(),
        topic = Word.apply(randomWord()),
        transactionHash = Word.apply(randomWord()),
        status = status,
        index = 0,
        logIndex = 0,
        minorLogIndex = 0
    )
}

fun randomErc20(value: EthUInt256) = Asset(Erc20AssetType(AddressFactory.create()), value)

fun randomErc20() = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(randomInt()))

fun randomEth() = Asset(EthAssetType, EthUInt256.of(randomInt()))

fun randomErc1155(value: EthUInt256) = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256(randomBigInt())), value)

fun randomErc721() = Asset(Erc721AssetType(AddressFactory.create(), EthUInt256(randomBigInt())), EthUInt256.ONE)

fun randomAmmNftAsset(token: Address = randomAddress()) = Asset(AmmNftAssetType(token), EthUInt256.ONE)

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

fun randomSeaportOffer(): SeaportOffer {
    return SeaportOffer(
        itemType = SeaportItemType.values().random(),
        token = randomAddress(),
        identifier = randomBigInt(),
        startAmount = randomBigInt(),
        endAmount = randomBigInt()
    )
}

fun randomSeaportConsideration(): SeaportConsideration {
    return SeaportConsideration(
        itemType = SeaportItemType.values().random(),
        token = randomAddress(),
        identifier = randomBigInt(),
        startAmount = randomBigInt(),
        endAmount = randomBigInt(),
        recipient = randomAddress()
    )
}
fun randomOrderBasicSeaportDataV1(): OrderBasicSeaportDataV1 {
    return OrderBasicSeaportDataV1(
        protocol = randomAddress(),
        orderType = SeaportOrderType.values().random(),
        offer = listOf(randomSeaportOffer(), randomSeaportOffer()),
        consideration = listOf(randomSeaportConsideration(), randomSeaportConsideration()),
        zone = randomAddress(),
        zoneHash = Word.apply(randomWord()),
        conduitKey = Word.apply(randomWord()),
        counter = randomLong()
    )
}

fun randomBidOrderUsdValue(): OrderUsdValue.BidOrder {
    return OrderUsdValue.BidOrder(
        makeUsd = randomBigDecimal(),
        takePriceUsd = randomBigDecimal()
    )
}

fun randomSellOrderUsdValue(): OrderUsdValue.SellOrder {
    return OrderUsdValue.SellOrder(
        makePriceUsd = randomBigDecimal(),
        takeUsd = randomBigDecimal()
    )
}

fun randomOrderEventDto(order: OrderDto = createOrderDto()): OrderUpdateEventDto {
    return OrderUpdateEventDto(
        eventId = randomString(),
        order = order,
        orderId = randomWord()
    )
}

fun randomSellOnChainAmmOrder(data: PoolData = createSudoSwapPoolDataV1()): PoolCreate {
    return PoolCreate(
        data = data,
        collection = randomAddress(),
        tokenIds = (1..10).map { EthUInt256.of(randomInt()) },
        currency = randomAddress(),
        currencyBalance = randomBigInt(),
        hash = Word.apply(randomWord()),
        date = Instant.now(),
        source = HistorySource.values().random()
    )
}

fun PoolHistory.isPoolCreate(): Boolean = this is PoolCreate

fun randomPoolTargetNftOut(): PoolTargetNftOut {
    return PoolTargetNftOut(
        hash = Word.apply(randomWord()),
        collection = randomAddress(),
        tokenIds = (1..10).map { EthUInt256(randomBigInt()) },
        recipient = randomAddress(),
        date = Instant.now(),
        source = HistorySource.values().random(),
        outputValue = EthUInt256.of(randomInt())
    )
}

fun randomPoolNftWithdraw(): PoolNftWithdraw {
    return PoolNftWithdraw(
        hash = Word.apply(randomWord()),
        tokenIds = (1..10).map { EthUInt256(randomBigInt()) },
        date = Instant.now(),
        source = HistorySource.values().random(),
        collection = randomAddress()
    )
}

fun randomPoolTargetNftIn(): PoolTargetNftIn {
    return PoolTargetNftIn(
        hash = Word.apply(randomWord()),
        collection = randomAddress(),
        tokenIds = (1..10).map { EthUInt256(randomBigInt()) },
        tokenRecipient = randomAddress(),
        date = Instant.now(),
        source = HistorySource.values().random(),
        inputValue = EthUInt256.of(randomInt())
    )
}

fun randomPoolNftDeposit(): PoolNftDeposit {
    return PoolNftDeposit(
        hash = Word.apply(randomWord()),
        tokenIds = (1..10).map { EthUInt256(randomBigInt()) },
        collection = randomAddress(),
        date = Instant.now(),
        source = HistorySource.values().random(),
    )
}

fun randomPoolSpotPriceUpdate(): PoolSpotPriceUpdate {
    return PoolSpotPriceUpdate(
        newSpotPrice = randomBigInt(),
        hash = Word.apply(randomWord()),
        date = Instant.now(),
        source = HistorySource.values().random(),
    )
}

fun randomPoolDeltaUpdate(): PoolDeltaUpdate {
    return PoolDeltaUpdate(
        newDelta = randomBigInt(),
        hash = Word.apply(randomWord()),
        date = Instant.now(),
        source = HistorySource.values().random(),
    )
}

fun randomPoolFeeUpdate(): PoolFeeUpdate {
    return PoolFeeUpdate(
        newFee = randomBigInt(),
        hash = Word.apply(randomWord()),
        date = Instant.now(),
        source = HistorySource.values().random(),
    )
}

fun randomSimpleTrace(): SimpleTraceResult {
    return SimpleTraceResult(
        from = randomAddress(),
        to = randomAddress(),
        input = Binary.empty(),
        value = randomBigInt(),
        output = Binary.empty()
    )
}

fun randomHeadTransaction(): HeadTransaction {
    return HeadTransaction(
        hash = Word.apply(randomWord()),
        input = randomBinary(),
        from = randomAddress(),
        to = randomAddress(),
        value = randomBigInt()
    )
}

fun randomPoolInfo(): PoolInfo {
    return PoolInfo(
        collection = randomAddress(),
        curve = randomAddress(),
        spotPrice = randomBigInt(),
        delta = randomBigInt(),
    )
}

