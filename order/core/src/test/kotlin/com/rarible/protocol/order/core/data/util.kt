package com.rarible.protocol.order.core.data

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.common.EventTimeMarks
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.LooksRareOrderDto
import com.rarible.protocol.dto.OrderBasicSeaportDataV1Dto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderLooksRareDataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV3BuyDto
import com.rarible.protocol.dto.OrderRaribleV2DataV3SellDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.dto.OrderX2Y2DataDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.dto.SeaportOrderTypeDto
import com.rarible.protocol.dto.SeaportV1OrderDto
import com.rarible.protocol.dto.X2Y2OrderDto
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.misc.toDto
import com.rarible.protocol.order.core.model.AmmNftAssetType
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc1155LazyAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Erc721LazyAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.GenerativeArtAssetType
import com.rarible.protocol.order.core.model.HeadTransaction
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.LooksrareQuoteType
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.OpenSeaOrderFeeMethod
import com.rarible.protocol.order.core.model.OpenSeaOrderHowToCall
import com.rarible.protocol.order.core.model.OpenSeaOrderSaleKind
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderUsdValue
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.PoolCreate
import com.rarible.protocol.order.core.model.PoolData
import com.rarible.protocol.order.core.model.PoolDeltaUpdate
import com.rarible.protocol.order.core.model.PoolFeeUpdate
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.PoolInfo
import com.rarible.protocol.order.core.model.PoolNftDeposit
import com.rarible.protocol.order.core.model.PoolNftWithdraw
import com.rarible.protocol.order.core.model.PoolSpotPriceUpdate
import com.rarible.protocol.order.core.model.PoolTargetNftIn
import com.rarible.protocol.order.core.model.PoolTargetNftOut
import com.rarible.protocol.order.core.model.SeaportConsideration
import com.rarible.protocol.order.core.model.SeaportItemType
import com.rarible.protocol.order.core.model.SeaportOffer
import com.rarible.protocol.order.core.model.SeaportOrderType
import com.rarible.protocol.order.core.model.SimpleTraceResult
import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import com.rarible.protocol.order.core.model.SudoSwapCurveType
import com.rarible.protocol.order.core.model.SudoSwapPoolDataV1
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import com.rarible.protocol.order.core.model.SudoSwapPurchaseValue
import com.rarible.protocol.order.core.model.SudoSwapSellInfo
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import org.bson.types.ObjectId
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit
import com.rarible.blockchain.scanner.ethereum.model.EventData as ScannerEventData

fun randomOrder(
    token: Address = AddressFactory.create(),
    tokenId: EthUInt256 = EthUInt256.TEN,
) =
    Order(
        maker = AddressFactory.create(),
        taker = null,
        make = Asset(Erc1155AssetType(token, tokenId), EthUInt256.TEN),
        take = Asset(
            Erc20AssetType(AddressFactory.create()),
            EthUInt256.of(BigInteger.valueOf(5) * BigInteger.valueOf(10).pow(18))
        ),
        makeStock = EthUInt256.TEN,
        type = OrderType.RARIBLE_V2,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.of(randomBigInt()),
        start = null,
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis()
    )

fun createSellOrder(
    data: OrderData = createOrderRaribleV2DataV1()
) = randomOrder().copy(make = randomErc721(), take = randomErc20(), data = data)

fun createBidOrder(
    data: OrderData = createOrderRaribleV2DataV1(),
    platform: Platform = Platform.RARIBLE,
) = randomOrder().copy(
    make = randomErc20(),
    take = randomErc721(),
    data = data,
    platform = platform
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
        end = nowMillis().plus(7, ChronoUnit.DAYS).epochSecond,
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

fun createOrderBasicSeaportDataV1(
    counter: Long = randomLong()
): OrderBasicSeaportDataV1 {
    return OrderBasicSeaportDataV1(
        protocol = randomAddress(),
        orderType = SeaportOrderType.SUPPORTED.random(),
        offer = emptyList(),
        consideration = emptyList(),
        zone = randomAddress(),
        zoneHash = Word.apply(randomWord()),
        conduitKey = Word.apply(randomWord()),
        counter = counter,
        counterHex = EthUInt256.of(counter)
    )
}

fun createOrderRaribleV2DataV1(): OrderRaribleV2DataV1 {
    return OrderRaribleV2DataV1(
        originFees = listOf(Part(randomAddress(), EthUInt256.TEN), Part(randomAddress(), EthUInt256.ONE)),
        payouts = listOf(Part(randomAddress(), EthUInt256.ONE), Part(randomAddress(), EthUInt256.TEN))
    )
}

fun createOrderRaribleV2DataV2(): OrderRaribleV2DataV2 {
    return OrderRaribleV2DataV2(
        originFees = listOf(Part(randomAddress(), EthUInt256.TEN), Part(randomAddress(), EthUInt256.ONE)),
        payouts = listOf(Part(randomAddress(), EthUInt256.ONE), Part(randomAddress(), EthUInt256.TEN)),
        isMakeFill = randomBoolean()
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

fun createOrderLooksrareDataV1(
    counter: Long = randomLong()
): OrderLooksrareDataV1 {
    return OrderLooksrareDataV1(
        minPercentageToAsk = randomInt(),
        strategy = randomAddress(),
        counter = counter,
        counterHex = EthUInt256.of(counter),
        params = randomBinary()
    )
}

fun createOrderLooksrareDataV2(
    counter: Long = randomLong()
): OrderLooksrareDataV2 {
    return OrderLooksrareDataV2(
        quoteType = LooksrareQuoteType.values().random(),
        orderNonce = EthUInt256.of(randomBigInt()),
        subsetNonce = EthUInt256.of(randomBigInt()),
        counterHex = EthUInt256.of(randomBigInt()),
        strategyId = EthUInt256.of(randomBigInt()),
        additionalParameters = Binary.empty(),
        merkleRoot = Binary.empty(),
        merkleProof = emptyList(),
    )
}

fun Order.withMakeFill(isMakeFill: Boolean = true): Order {
    val newData = data.withMakeFill(isMakeFill)
    val hash = Order.hashKey(maker, make.type, take.type, salt.value, newData)
    return copy(
        data = newData,
        id = Order.Id(hash),
        hash = hash
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

fun createOnChainOrder(): OnChainOrder {
    return OnChainOrder(
        maker = randomAddress(),
        taker = null,
        make = randomErc721(),
        take = randomErc20(),
        orderType = OrderType.RARIBLE_V2,
        salt = EthUInt256.of(randomBigInt()),
        start = null,
        end = null,
        data = createOrderRaribleV1DataV3Buy(),
        signature = null,
        createdAt = nowMillis(),
        platform = Platform.RARIBLE,
        priceUsd = null,
        hash = Word.apply(randomWord())
    )
}

fun createLogEvent(
    data: EventData,
    status: EthereumBlockStatus = EthereumBlockStatus.CONFIRMED,
    blockNumber: Long = 0
): ReversedEthereumLogRecord {
    return ReversedEthereumLogRecord(
        id = ObjectId().toHexString(),
        data = data,
        address = randomAddress(),
        topic = Word.apply(randomWord()),
        transactionHash = randomWord(),
        status = status,
        blockNumber = blockNumber,
        index = 0,
        logIndex = 0,
        minorLogIndex = 0
    )
}

fun randomErc20Type() = Erc20AssetType(AddressFactory.create())

fun randomErc20(value: EthUInt256) = Asset(Erc20AssetType(AddressFactory.create()), value)

fun randomErc20(token: Address = randomAddress()) = Asset(Erc20AssetType(token), EthUInt256.of(randomInt()))

fun randomEth() = Asset(EthAssetType, EthUInt256.of(randomInt()))

fun randomErc721Type(token: Address = randomAddress(), tokenId: EthUInt256 = EthUInt256.of(randomBigInt())) =
    Erc721AssetType(token, tokenId)

fun randomCollectionType(token: Address = randomAddress()) = CollectionAssetType(token)

fun randomErc1155Type() = Erc1155AssetType(AddressFactory.create(), EthUInt256(randomBigInt()))

fun randomErc1155(value: EthUInt256) = Asset(randomErc1155Type(), value)

fun randomErc721(token: Address = randomAddress(), tokenId: EthUInt256 = EthUInt256.of(randomBigInt())) =
    Asset(randomErc721Type(token, tokenId), EthUInt256.ONE)

fun randomCollection(token: Address = randomAddress()) = Asset(randomCollectionType(token), EthUInt256.ONE)

fun randomAmmNftAsset(token: Address = randomAddress()) = Asset(AmmNftAssetType(token), EthUInt256.ONE)

fun randomPart() = Part(randomAddress(), EthUInt256(randomBigInt()))

fun randomPartDto() = PartDto(randomAddress(), randomInt())

fun randomCryptoPunksAssetType() =
    CryptoPunksAssetType(
        token = AddressFactory.create(),
        tokenId = EthUInt256(randomBigInt()),
    )

fun randomCollectionType() = CollectionAssetType(token = AddressFactory.create())

fun randomAmmNftType() = AmmNftAssetType(token = AddressFactory.create())

fun randomGenerativeArtAssetType() = GenerativeArtAssetType(token = AddressFactory.create())

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
    val counter = randomLong()
    return OrderBasicSeaportDataV1(
        protocol = randomAddress(),
        orderType = SeaportOrderType.SUPPORTED.random(),
        offer = listOf(randomSeaportOffer(), randomSeaportOffer()),
        consideration = listOf(randomSeaportConsideration(), randomSeaportConsideration()),
        zone = randomAddress(),
        zoneHash = Word.apply(randomWord()),
        conduitKey = Word.apply(randomWord()),
        counter = counter,
        counterHex = EthUInt256.of(counter),
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
        orderId = randomWord(),
        eventTimeMarks = orderOffchainEventMarks().toDto()
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
        fee = randomBigInt(),
        protocolFee = randomBigInt(),
        token = randomAddress()
    )
}

fun randomSudoSwapPurchaseValue(): SudoSwapPurchaseValue {
    return SudoSwapPurchaseValue(
        newSpotPrice = randomBigInt(),
        newDelta = randomBigInt(),
        value = randomBigInt(),
    )
}

fun randomApproveHistory(
    collection: Address = randomAddress(),
    owner: Address = randomAddress(),
    operator: Address = randomAddress(),
    approved: Boolean = true
): ApprovalHistory {
    return ApprovalHistory(
        collection = collection,
        owner = owner,
        operator = operator,
        approved = approved,
    )
}

fun createRandomEthereumLog(
    transactionSender: Address = randomAddress()
): EthereumLog =
    EthereumLog(
        transactionHash = randomWord(),
        status = EthereumBlockStatus.values().random(),
        address = randomAddress(),
        topic = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        minorLogIndex = randomInt(),
        index = randomInt(),
        from = transactionSender,
        blockTimestamp = nowMillis().epochSecond,
        createdAt = nowMillis()
    )

fun createLogRecord(data: ScannerEventData): ReversedEthereumLogRecord =
    ReversedEthereumLogRecord(
        id = randomString(),
        version = randomLong(),
        data = data,
        log = createRandomEthereumLog()
    )

fun createLogRecordEvent(data: ScannerEventData): LogRecordEvent {
    val record = createLogRecord(data)
    return LogRecordEvent(
        record = record,
        reverted = record.status == EthereumBlockStatus.REVERTED,
        eventTimeMarks = EventTimeMarks("test")
    )
}

fun createChangeNonceHistory(nonce: Long): ChangeNonceHistory {
    return ChangeNonceHistory(
        maker = randomAddress(),
        newNonce = EthUInt256.of(nonce),
        date = Instant.now(),
        source = HistorySource.values().random()
    )
}