package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.Tuples.keccak256
import com.rarible.protocol.order.core.misc.plus
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.misc.zeroWord
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import scala.Tuple10
import scala.Tuple22
import scala.Tuple3
import scala.Tuple4
import scala.Tuple5
import scala.Tuple9
import scalether.abi.AddressType
import scalether.abi.Uint256Type
import scalether.abi.Uint8Type
import scalether.domain.Address
import scalether.util.Hash
import scalether.util.Hex
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time.Instant
import org.springframework.data.annotation.Id as SpringDataId

@Document(MongoOrderRepository.COLLECTION)
data class Order(
    val maker: Address,
    val taker: Address?,

    val make: Asset,
    val take: Asset,

    val type: OrderType,

    val fill: EthUInt256,
    val cancelled: Boolean,

    val makeStock: EthUInt256,

    val salt: EthUInt256,
    val start: Long?,
    val end: Long?,
    val data: OrderData,
    val signature: Binary?,

    val createdAt: Instant,
    val lastUpdateAt: Instant,
    //TODO after migration ALPHA-405 can't be null
    val dbUpdatedAt: Instant? = null,

    @Deprecated("Should be removed in the latest releases")
    val pending: List<OrderExchangeHistory> = emptyList(),

    @Deprecated("Please don't use. Will be removed in future releases.")
    val makePriceUsd: BigDecimal? = null,
    @Deprecated("Please don't use. Will be removed in future releases.")
    val takePriceUsd: BigDecimal? = null,
    val makePrice: BigDecimal? = null,
    val takePrice: BigDecimal? = null,
    @Deprecated("Please don't use. Will be removed in future releases.")
    val makeUsd: BigDecimal? = null,
    @Deprecated("Please don't use. Will be removed in future releases.")
    val takeUsd: BigDecimal? = null,
    val priceHistory: List<OrderPriceHistoryRecord> = emptyList(),

    // TODO: this field is 'var' and calculated in the constructor to make sure it is recalculated on any Order.copy()
    //  Is there a better way of storing "computable" fields in the Mongo, but ignoring them on deserialization?
    var status: OrderStatus = OrderStatus.ACTIVE,

    val platform: Platform = Platform.RARIBLE,

    val lastEventId: String? = null,

    @SpringDataId
    val id: Id = Id(hashKey(maker, make.type, take.type, salt.value, data)),

    val hash: Word = id.hash,

    @Version
    val version: Long? = null,

    /**
     * Has been ApproveForAll or Approve event applied for sale/bid token
     */
    val approved: Boolean = true,
    /**
     * With flag need to expire orders in advance
     */
    val advanceExpired: Boolean? = null
) {
    init {
        status = calculateStatus(fill, make, take, makeStock, cancelled, start, end, data, approved, advanceExpired)
    }

    fun forV1Tx() = run {
        assert(type == OrderType.RARIBLE_V1)
        assert(data is OrderDataLegacy)
        val makeLegacy = make.type.toLegacy()
            ?: throw IllegalArgumentException("not supported asset type for V1 order: ${make.type}")
        val takeLegacy = take.type.toLegacy()
            ?: throw IllegalArgumentException("not supported asset type for V1 order: ${take.type}")
        val orderKey = Tuple4(maker, salt.value, makeLegacy.toTuple(), takeLegacy.toTuple())
        Tuple4(orderKey, make.value.value, take.value.value, (data as OrderDataLegacy).fee.toBigInteger())
    }

    private fun LegacyAssetType.toTuple() = Tuple3(token, tokenId, clazz.value)

    fun forTx(wrongDataEncode: Boolean = false) = Tuple9(
        maker,
        make.forTx(),
        taker ?: Address.ZERO(),
        take.forTx(),
        salt.value,
        start?.toBigInteger() ?: BigInteger.ZERO,
        end?.toBigInteger() ?: BigInteger.ZERO,
        data.toDataVersion(),
        data.toEthereum(wrongDataEncode).bytes()
    )

    fun withMakeBalance(
        makeBalance: EthUInt256,
        protocolCommission: EthUInt256
    ): Order {
        return copy(
            makeStock = calculateMakeStock(
                makeValue = make.value,
                takeValue = take.value,
                fill = fill,
                data = data,
                makeBalance = makeBalance,
                protocolCommission = protocolCommission,
                cancelled = cancelled,
                orderType = type,
                feeSide = getFeeSide(make.type, take.type),
                sell = make.type.nft
            )
        )
    }

    fun withOrderUsdValue(usdValue: OrderUsdValue): Order {
        return copy(
            takePriceUsd = usdValue.takePriceUsd,
            makePriceUsd = usdValue.makePriceUsd,
            takeUsd = usdValue.takeUsd,
            makeUsd = usdValue.makeUsd
        )
    }

    fun withDbUpdated(): Order {
        return copy(dbUpdatedAt = nowMillis())
    }

    fun withUpdatedStatus(updateTime: Instant = nowMillis()): Order {
        return copy(
            status = calculateStatus(fill, make, take, makeStock, cancelled, start, end, data, approved, advanceExpired),
            lastUpdateAt = updateTime
        )
    }

    fun withApproved(approved: Boolean): Order {
        return copy(approved = approved)
    }

    fun withAdvanceExpired(advanceExpired: Boolean?): Order {
        return copy(advanceExpired = advanceExpired)
    }

    fun isEnded() = Companion.isEnded(end)

    fun isEndedAt(at: Instant) = Companion.isEnded(end, at.epochSecond)

    fun isEndedBid(): Boolean {
        return isBid() && isEnded()
    }

    fun isOptionalRoyalties(): Boolean {
        return when (platform) {
            Platform.SUDOSWAP -> true
            Platform.RARIBLE,
            Platform.OPEN_SEA,
            Platform.CRYPTO_PUNKS,
            Platform.LOOKSRARE,
            Platform.X2Y2,
            Platform.BLUR -> false
        }
    }

    fun isLegacyOpenSea(exchange: Address): Boolean {
        return if (this.type == OrderType.OPEN_SEA_V1) {
            when (data) {
                is OrderOpenSeaV1DataV1 -> data.exchange == exchange
                is OrderCryptoPunksData,
                is OrderDataLegacy,
                is OrderRaribleV2DataV1,
                is OrderRaribleV2DataV2,
                is OrderRaribleV2DataV3Buy,
                is OrderRaribleV2DataV3Sell,
                is OrderBasicSeaportDataV1,
                is OrderLooksrareDataV1,
                is OrderLooksrareDataV2,
                is OrderX2Y2DataV1,
                is OrderSudoSwapAmmDataV1 -> false
            }
        } else {
            false
        }
    }

    fun withVersion(version: Long?): Order {
        return copy(version = version)
    }

    fun withFinalState(state: OrderState): Order {
        return copy(
            cancelled = state.canceled,
            lastUpdateAt = maxOf(lastUpdateAt, state.lastUpdateAt),
            lastEventId = accumulateEventId(lastEventId, state.id.prefixed())
        )
    }

    fun cancelEndedBid(): Order {
        return if (isEndedBid()) withCancel(true) else this
    }

    fun withCancel(cancel: Boolean): Order {
        return copy(cancelled = cancel)
    }

    fun toOrderVersion(): OrderVersion = OrderVersion(
        maker = maker,
        make = make,
        take = take,
        taker = taker,
        type = type,
        salt = salt,
        start = start,
        end = end,
        data = data,
        signature = signature,
        platform = platform,
        hash = hash,
        approved = approved,
        createdAt = createdAt,
        makePriceUsd = null,
        takePriceUsd = null,
        makePrice = null,
        takePrice = null,
        makeUsd = null,
        takeUsd = null
    )

    companion object {

        /**
         * Maximum size of [priceHistory]
         */
        const val MAX_PRICE_HISTORIES = 20

        private fun calculateMakeStock(
            makeValue: EthUInt256,
            takeValue: EthUInt256,
            fill: EthUInt256,
            data: OrderData,
            makeBalance: EthUInt256,
            protocolCommission: EthUInt256,
            orderType: OrderType,
            feeSide: FeeSide,
            cancelled: Boolean,
            sell: Boolean
        ): EthUInt256 {
            if (makeValue == EthUInt256.ZERO || takeValue == EthUInt256.ZERO) {
                return EthUInt256.ZERO
            }
            val (make) = calculateRemaining(makeValue, takeValue, fill, cancelled, data, sell)
            val fee = if (feeSide == FeeSide.MAKE) calculateFee(data, protocolCommission) else EthUInt256.ZERO

            val roundedMakeBalance = calculateRoundedMakeBalance(
                makeValue = makeValue,
                takeValue = takeValue,
                makeBalance = (makeBalance * EthUInt256.of(10000)) / (fee + EthUInt256.of(10000))
            )
            val calculatedMakeStock = minOf(make, roundedMakeBalance)
            return when (orderType) {
                OrderType.RARIBLE_V2,
                OrderType.RARIBLE_V1,
                OrderType.CRYPTO_PUNKS -> calculatedMakeStock
                OrderType.LOOKSRARE,
                OrderType.LOOKSRARE_V2,
                OrderType.X2Y2,
                OrderType.OPEN_SEA_V1,
                OrderType.SEAPORT_V1 -> if (make > roundedMakeBalance) EthUInt256.ZERO else calculatedMakeStock
                OrderType.AMM -> makeBalance
            }
        }

        private fun calculateStatus(
            fill: EthUInt256,
            make: Asset,
            take: Asset,
            makeStock: EthUInt256,
            cancelled: Boolean,
            start: Long?,
            end: Long?,
            data: OrderData,
            approved: Boolean,
            advanceExpired: Boolean?,
        ): OrderStatus {
            return when {
                data.isAmmOrder() -> ammOrderStatus(makeStock, start, end)
                data.isMakeFillOrder(make.type.nft) && fill >= make.value -> OrderStatus.FILLED
                fill >= take.value -> OrderStatus.FILLED
                cancelled -> OrderStatus.CANCELLED
                approved.not() -> OrderStatus.INACTIVE
                advanceExpired == true -> OrderStatus.ENDED
                isActiveByMakeStock(makeStock, start, end) -> OrderStatus.ACTIVE
                !isStarted(start) -> OrderStatus.NOT_STARTED
                isEnded(end) -> OrderStatus.ENDED
                else -> OrderStatus.INACTIVE
            }
        }

        private fun isActiveByMakeStock(makeStock: EthUInt256, start: Long?, end: Long?): Boolean {
            return makeStock > EthUInt256.ZERO && isAlive(start, end)
        }

        private fun ammOrderStatus(makeStock: EthUInt256, start: Long?, end: Long?): OrderStatus {
            return if (isActiveByMakeStock(makeStock, start, end)) OrderStatus.ACTIVE else OrderStatus.INACTIVE
        }

        private fun isAlive(start: Long?, end: Long?) = isStarted(start) && !isEnded(end)

        private fun isEnded(end: Long?): Boolean {
            return isEnded(end, Instant.now().epochSecond)
        }


        private fun isEnded(end: Long?, now: Long): Boolean {
            return end?.let { it in 1 until now } ?: false
        }

        private fun isStarted(start: Long?): Boolean {
            val now = Instant.now().epochSecond
            return start?.let { it <= now } ?: true
        }

        private fun calculateRemaining(
            makeValue: EthUInt256,
            takeValue: EthUInt256,
            fill: EthUInt256,
            cancelled: Boolean,
            data: OrderData,
            sell: Boolean
        ): Pair<EthUInt256, EthUInt256> {
            return if (cancelled) {
                EthUInt256.ZERO to EthUInt256.ZERO
            } else if (data.isMakeFillOrder(sell)) {
                val make = if (makeValue > fill) makeValue - fill else EthUInt256.ZERO
                val take = make * takeValue / makeValue
                make to take
            } else {
                val take = if (takeValue > fill) takeValue - fill else EthUInt256.ZERO
                val make = take * makeValue / takeValue
                make to take
            }
        }

        private fun calculateFee(data: OrderData, protocolCommission: EthUInt256): EthUInt256 {
            fun calculate(fees: List<Part>): EthUInt256 {
                return fees.fold(protocolCommission) { acc, part -> acc + part.value }
            }
            return when (data) {
                is OrderRaribleV2DataV1 -> calculate(data.originFees)
                is OrderRaribleV2DataV2 -> calculate(data.originFees)
                is OrderRaribleV2DataV3 -> calculate(listOfNotNull(data.originFeeFirst, data.originFeeSecond))
                is OrderDataLegacy -> EthUInt256.of(data.fee.toLong())
                is OrderOpenSeaV1DataV1 -> EthUInt256.ZERO
                is OrderCryptoPunksData -> EthUInt256.ZERO
                is OrderBasicSeaportDataV1 -> EthUInt256.ZERO
                is OrderX2Y2DataV1 -> EthUInt256.ZERO
                is OrderLooksrareDataV1 -> EthUInt256.ZERO
                is OrderLooksrareDataV2 -> EthUInt256.ZERO
                is OrderSudoSwapAmmDataV1 -> EthUInt256.ZERO
            }
        }

        private fun calculateRoundedMakeBalance(
            makeValue: EthUInt256,
            takeValue: EthUInt256,
            makeBalance: EthUInt256
        ): EthUInt256 {
            val maxTake = makeBalance * takeValue / makeValue
            return makeValue * maxTake / takeValue
        }

        fun hash(order: Order): Word = with(order) {
            hash(maker, make, taker, take, salt.value, start, end, data, type)
        }

        fun hash(orderVersion: OrderVersion): Word = with(orderVersion) {
            hash(maker, make, taker, take, salt.value, start, end, data, type)
        }

        fun hash(
            maker: Address,
            make: Asset,
            taker: Address?,
            take: Asset,
            salt: BigInteger,
            start: Long?,
            end: Long?,
            data: OrderData,
            type: OrderType
        ): Word {
            return when (type) {
                OrderType.RARIBLE_V2 -> raribleExchangeV2Hash(maker, make, taker, take, salt, start, end, data)
                OrderType.RARIBLE_V1 -> raribleExchangeV1Hash(maker, make, take, salt, data)
                OrderType.OPEN_SEA_V1 -> openSeaV1Hash(maker, make, taker, take, salt, start, end, data)
                OrderType.SEAPORT_V1,
                OrderType.LOOKSRARE,
                OrderType.LOOKSRARE_V2,
                OrderType.CRYPTO_PUNKS,
                OrderType.X2Y2,
                OrderType.AMM -> throw IllegalArgumentException("Can't calculate $type order hash")
            }
        }

        fun Order.legacyMessage(): String {
            return legacyMessage(maker, make, take, salt.value, data)
        }

        fun OrderVersion.legacyMessage(): String {
            return legacyMessage(maker, make, take, salt.value, data)
        }

        private fun legacyMessage(maker: Address, make: Asset, take: Asset, salt: BigInteger, data: OrderData): String {
            val legacyMakeAsset = make.type.toLegacy()
                ?: error("Unsupported make asset ${make.type} by legacy contract")
            val legacyTakeAsset = take.type.toLegacy()
                ?: error("Unsupported take asset ${take.type} by legacy contract")
            val legacyData = (data as? OrderDataLegacy) ?: error("Unsupported data for legacy contract")

            val binary = Tuples.legacyOrderHashType().encode(
                Tuple4(
                    Tuple4(
                        maker,
                        salt,
                        Tuple3(
                            legacyMakeAsset.token,
                            legacyMakeAsset.tokenId,
                            legacyMakeAsset.clazz.value
                        ),
                        Tuple3(
                            legacyTakeAsset.token,
                            legacyTakeAsset.tokenId,
                            legacyTakeAsset.clazz.value
                        )
                    ),
                    make.value.value,
                    take.value.value,
                    legacyData.fee.toBigInteger()
                )
            )
            val hash = Hash.sha3(binary.bytes())
            return Hex.to(hash)
        }

        fun raribleExchangeV1Hash(
            maker: Address,
            make: Asset,
            take: Asset,
            salt: BigInteger,
            data: OrderData
        ): Word {
            val legacyMakeAsset = make.type.toLegacy()
                ?: error("Unsupported make asset ${make.type} by legacy contract")
            val legacyTakeAsset = take.type.toLegacy()
                ?: error("Unsupported take asset ${take.type} by legacy contract")
            val legacyData = (data as? OrderDataLegacy) ?: error("Unsupported data for legacy contract")

            val binary = Tuples.legacyOrderHashType().encode(
                Tuple4(
                    Tuple4(
                        maker,
                        salt,
                        Tuple3(
                            legacyMakeAsset.token,
                            legacyMakeAsset.tokenId,
                            legacyMakeAsset.clazz.value
                        ),
                        Tuple3(
                            legacyTakeAsset.token,
                            legacyTakeAsset.tokenId,
                            legacyTakeAsset.clazz.value
                        )
                    ),
                    make.value.value,
                    take.value.value,
                    legacyData.fee.toBigInteger()
                )
            )
            val hash = Hash.sha3(binary.bytes())
            return Word.apply(hash)
        }

        fun raribleExchangeV2Hash(
            maker: Address,
            make: Asset,
            taker: Address?,
            take: Asset,
            salt: BigInteger,
            start: Long?,
            end: Long?,
            data: OrderData
        ): Word {
            return keccak256(
                Tuples.orderHashType().encode(
                    Tuple10(
                        TYPE_HASH.bytes(),
                        maker,
                        Asset.hash(make).bytes(),
                        taker ?: Address.ZERO(),
                        Asset.hash(take).bytes(),
                        salt,
                        (start ?: 0).toBigInteger(),
                        (end ?: 0).toBigInteger(),
                        data.toDataVersion(),
                        keccak256(data.toEthereum()).bytes()
                    )
                )
            )
        }

        fun openSeaV1Hash(
            maker: Address,
            make: Asset,
            taker: Address?,
            take: Asset,
            salt: BigInteger,
            start: Long?,
            end: Long?,
            data: OrderData
        ): Word {
            val openSeaData = (data as? OrderOpenSeaV1DataV1)
                ?: error("Unsupported data type ${data.javaClass} for OpenSea contract")
            val nftAsset = when {
                make.type.nft -> make.type
                take.type.nft -> take.type
                else -> throw UnsupportedOperationException("Unsupported exchange assets pairs, can't find NFT asset type")
            }
            val paymentAsset = when {
                make.type.nft.not() -> make
                take.type.nft.not() -> take
                else -> throw UnsupportedOperationException("Unsupported exchange assets pairs, can't find payment asset type")
            }
            return openSeaV1Hash(
                maker = maker,
                taker = taker,
                nftToken = nftAsset.token,
                paymentToken = paymentAsset.type.token,
                basePrice = paymentAsset.value.value,
                salt = salt,
                start = start,
                end = end,
                data = openSeaData
            )
        }

        fun openSeaV1Hash(
            maker: Address,
            taker: Address?,
            nftToken: Address,
            paymentToken: Address,
            basePrice: BigInteger,
            salt: BigInteger,
            start: Long?,
            end: Long?,
            data: OrderOpenSeaV1DataV1
        ): Word {
            return keccak256(
                data.exchange
                    .add(maker)
                    .add(taker ?: Address.ZERO())
                    .add(Uint256Type.encode(data.makerRelayerFee))
                    .add(Uint256Type.encode(data.takerRelayerFee))
                    .add(Uint256Type.encode(data.makerProtocolFee))
                    .add(Uint256Type.encode(data.takerProtocolFee))
                    .add(data.feeRecipient)
                    .add(Uint8Type.encode(data.feeMethod.value).bytes().sliceArray(31..31))
                    .add(Uint8Type.encode(data.side.value).bytes().sliceArray(31..31))
                    .add(Uint8Type.encode(data.saleKind.value).bytes().sliceArray(31..31))
                    .add(data.target ?: nftToken)
                    .add(Uint8Type.encode(data.howToCall.value).bytes().sliceArray(31..31))
                    .add(data.callData)
                    .add(data.replacementPattern)
                    .add(data.staticTarget)
                    .add(data.staticExtraData)
                    .add(paymentToken)
                    .add(Uint256Type.encode(basePrice))
                    .add(Uint256Type.encode(data.extra))
                    .add(Uint256Type.encode(start?.toBigInteger() ?: BigInteger.ZERO))
                    .add(Uint256Type.encode(end?.toBigInteger() ?: BigInteger.ZERO))
                    .add(Uint256Type.encode(salt))
            )
        }

        fun openSeaV1EIP712Hash(
            maker: Address,
            taker: Address?,
            paymentToken: Address,
            basePrice: BigInteger,
            salt: BigInteger,
            start: Long?,
            end: Long?,
            data: OrderOpenSeaV1DataV1
        ): Word {
            val p1 = Tuples.openseaV1HashTypeP1().encode(
                Tuple22(
                    OPEN_SEA_ORDER_TYPE_HASH.bytes(),
                    data.exchange,
                    maker,
                    taker ?: Address.ZERO(),
                    data.makerRelayerFee,
                    data.takerRelayerFee,
                    data.makerProtocolFee,
                    data.takerProtocolFee,
                    data.feeRecipient,
                    data.feeMethod.value,
                    data.side.value,
                    data.saleKind.value,
                    data.target!!,
                    data.howToCall.value,
                    keccak256(data.callData).bytes(),
                    keccak256(data.replacementPattern).bytes(),
                    data.staticTarget,
                    keccak256(data.staticExtraData).bytes(),
                    paymentToken,
                    basePrice,
                    data.extra,
                    (start ?: 0).toBigInteger()
                )
            )
            val p2 = Tuples.openseaV1HashTypeP2().encode(
                Tuple3(
                    (end ?: 0).toBigInteger(),
                    salt,
                    BigInteger.valueOf(data.nonce!!)
                )
            )
            return keccak256(p1 + p2)
        }

        fun seaportV1Hash(
            components: SeaportOrderComponents,
        ): Word = seaportV1Hash(components, components.counter)

        fun seaportV1Hash(
            data: SeaportOrderData,
            counter: BigInteger
        ): Word {
            val offerHash = data.offer.map { offer ->
                keccak256(
                    OFFER_ITEM_TYPE_HASH
                        .add(Uint256Type.encode(offer.itemType.value.toBigInteger()))
                        .add(AddressType.encode(offer.token))
                        .add(Uint256Type.encode(offer.identifier))
                        .add(Uint256Type.encode(offer.startAmount))
                        .add(Uint256Type.encode(offer.endAmount))
                )
            }.fold(Binary.empty()) { acc, next -> acc.add(next) }.let { keccak256(it) }

            val considerationHash = data.consideration.map { consideration ->
                keccak256(
                    CONSIDERATION_ITEM_TYPE_HASH
                        .add(Uint256Type.encode(consideration.itemType.value.toBigInteger()))
                        .add(AddressType.encode(consideration.token))
                        .add(Uint256Type.encode(consideration.identifier))
                        .add(Uint256Type.encode(consideration.startAmount))
                        .add(Uint256Type.encode(consideration.endAmount))
                        .add(AddressType.encode(consideration.recipient))
                )
            }.fold(Binary.empty()) { acc, next -> acc.add(next) }.let { keccak256(it) }

            return keccak256(
                ORDER_TYPE_HASH
                    .add(AddressType.encode(data.offerer))
                    .add(AddressType.encode(data.zone))
                    .add(offerHash)
                    .add(considerationHash)
                    .add(Uint256Type.encode(data.orderType.value.toBigInteger()))
                    .add(Uint256Type.encode((data.startTime)))
                    .add(Uint256Type.encode((data.endTime)))
                    .add(data.zoneHash)
                    .add(Uint256Type.encode(data.salt))
                    .add(data.conduitKey)
                    .add(Uint256Type.encode(counter))
            )
        }

        fun seaportV1Hash(
            maker: Address,
            salt: BigInteger,
            start: Long?,
            end: Long?,
            data: OrderSeaportDataV1
        ): Word {
            return seaportV1Hash(
                SeaportOrderComponents(
                    offerer = maker,
                    zone = data.zone,
                    offer = data.offer,
                    consideration = data.consideration,
                    orderType = data.orderType,
                    startTime = start?.toBigInteger() ?: BigInteger.ZERO,
                    endTime = end?.toBigInteger() ?: BigInteger.ZERO,
                    zoneHash = data.zoneHash,
                    salt = salt,
                    conduitKey = data.conduitKey,
                    counter = data.getCounterValue().value
                )
            )
        }

        /**
         * function _hashOrder(Order calldata order, uint256 nonce)
         *         internal
         *         pure
         *         returns (bytes32)
         *     {
         *         return keccak256(
         *             bytes.concat(
         *                 abi.encode(
         *                       ORDER_TYPEHASH,
         *                       order.trader,
         *                       order.side,
         *                       order.matchingPolicy,
         *                       order.collection,
         *                       order.tokenId,
         *                       order.amount,
         *                       order.paymentToken,
         *                       order.price,
         *                       order.listingTime,
         *                       order.expirationTime,
         *                       _packFees(order.fees),
         *                       order.salt,
         *                       keccak256(order.extraParams)
         *                 ),
         *                 abi.encode(nonce)
         *             )
         *         );
         *     }
         */
        fun blurV1Hash(
            blurOrder: BlurOrder,
            counter: BigInteger,
        ): Word {
            fun feeHash(fee: BlurFee): Word {
                return keccak256(
                    BLUR_FEE_TYPE_HASH
                        .add(Uint256Type.encode(fee.rate))
                        .add(AddressType.encode(fee.recipient))
                )
            }

            fun packFees(fees: List<BlurFee>): Binary {
                return fees.fold(Binary.empty()) { acc, next ->
                    acc.add(feeHash(next))
                }
            }
            return keccak256(
                BLUR_ORDER_TYPE_HASH
                    .add(AddressType.encode(blurOrder.trader))
                    .add(Uint256Type.encode((blurOrder.side.value)))
                    .add(AddressType.encode((blurOrder.matchingPolicy)))
                    .add(AddressType.encode((blurOrder.collection)))
                    .add(Uint256Type.encode((blurOrder.tokenId)))
                    .add(Uint256Type.encode((blurOrder.amount)))
                    .add(AddressType.encode((blurOrder.paymentToken)))
                    .add(Uint256Type.encode((blurOrder.price)))
                    .add(Uint256Type.encode((blurOrder.listingTime)))
                    .add(Uint256Type.encode((blurOrder.expirationTime)))
                    .add(keccak256(packFees(blurOrder.fees)))
                    .add(Uint256Type.encode((blurOrder.salt)))
                    .add(keccak256(blurOrder.extraParams))
                    .add(Uint256Type.encode((counter)))
            )
        }

        fun hashKey(
            maker: Address,
            makeAssetType: AssetType,
            takeAssetType: AssetType,
            salt: BigInteger,
            data: OrderData? = null
        ): Word = when (data) {
            is OrderRaribleV2DataV2, is OrderRaribleV2DataV3 -> {
                // For RaribleV2 DataV2/DataV3 hash contains the data bytes.
                keccak256(
                    Tuples.orderKeyHashTypeDataV2().encode(
                        Tuple5(
                            maker,
                            AssetType.hash(makeAssetType).bytes(),
                            AssetType.hash(takeAssetType).bytes(),
                            salt,
                            data.toEthereum().bytes()
                        )
                    )
                )
            }
            else -> {
                keccak256(
                    Tuples.orderKeyHashTypeDataV1().encode(
                        Tuple4(
                            maker,
                            AssetType.hash(makeAssetType).bytes(),
                            AssetType.hash(takeAssetType).bytes(),
                            salt
                        )
                    )
                )
            }
        }

        private val TYPE_HASH = keccak256(
            "Order(address maker,Asset makeAsset,address taker,Asset takeAsset,uint256 salt,uint256 start,uint256 end,bytes4 dataType,bytes data)Asset(AssetType assetType,uint256 value)AssetType(bytes4 assetClass,bytes data)"
        )

        private val OPEN_SEA_ORDER_TYPE_HASH = Word.apply(
            "0xdba08a88a748f356e8faf8578488343eab21b1741728779c9dcfdc782bc800f8"
        )

        fun openSeaV1EIP712HashToSign(hash: Word, domain: Word): Word {
            return Word(
                Hash.sha3(
                    Binary.apply("0x1901")
                        .add(domain)
                        .add(hash).bytes()
                )
            )
        }

        fun getFeeSide(make: AssetType, take: AssetType): FeeSide {
            return when {
                make is EthAssetType -> FeeSide.MAKE
                take is EthAssetType -> FeeSide.TAKE
                make is Erc20AssetType -> FeeSide.MAKE
                take is Erc20AssetType -> FeeSide.TAKE
                make is Erc1155AssetType -> FeeSide.MAKE
                take is Erc1155AssetType -> FeeSide.TAKE
                else -> FeeSide.NONE
            }
        }

        fun accumulateEventId(lastEventId: String?, eventId: String): String {
            val bytes = ((lastEventId ?: "") + eventId).toByteArray()
            return Binary.apply(Hash.sha3(bytes)).hex()
        }
    }

    data class Id(
        val hash: Word,
        val version: Long? = null
    ) {
        override fun toString(): String = version?.let { "${hash.prefixed()}$DIVIDER$it" } ?: hash.prefixed()

        companion object {
            private const val DIVIDER = "_"

            fun String.toOrderId(): Id {
                val parts = this.split(DIVIDER)

                return when (parts.size) {
                    2 -> Id(Word.apply(parts[0]), parts[1].toLong())
                    1 -> Id(Word.apply(this))
                    else -> throw IllegalArgumentException(
                        "OrderId=$this has invalid format. It must be in format of hash or hash${DIVIDER}version"
                    )
                }
            }
        }
    }
}

fun Order.invert(
    maker: Address,
    amount: BigInteger,
    newSalt: Word = zeroWord(),
    newData: OrderData = this.data
): Order = run {
    val (makeValue, takeValue) = calculateAmounts(make.value.value, take.value.value, amount, isBid())
    val hash = Order.hashKey(maker, take.type, make.type, salt.value, newData)
    this.copy(
        maker = maker,
        taker = this.maker,
        make = take.copy(value = EthUInt256(makeValue)),
        take = make.copy(value = EthUInt256(takeValue)),
        data = newData,
        id = Order.Id(hash),
        hash = hash,
        salt = EthUInt256.of(newSalt),
        makeStock = EthUInt256.ZERO,
        signature = null,
        lastUpdateAt = nowMillis()
    )
}

fun Order.isBid() = take.type.nft

fun Order.isSell() = make.type.nft

fun Order.nft(): Asset = if (isSell()) {
    make
} else {
    take
}

fun Order.payment(): Asset = if (isSell()) {
    take
} else {
    make
}

fun calculateAmounts(
    make: BigInteger,
    take: BigInteger,
    amount: BigInteger,
    bid: Boolean
): Pair<BigInteger, BigInteger> {
    return if (bid) {
        Pair(amount, amount.multiply(make).div(take))
    } else {
        Pair(amount.multiply(take).div(make), amount)
    }
}

val AssetType.token: Address
    get() {
        return when (this) {
            is Erc721AssetType -> token
            is Erc1155AssetType -> token
            is Erc1155LazyAssetType -> token
            is Erc20AssetType -> token
            is Erc721LazyAssetType -> token
            is GenerativeArtAssetType -> Address.ZERO()
            is CryptoPunksAssetType -> token
            is CollectionAssetType -> token
            is AmmNftAssetType -> token
            is EthAssetType -> Address.ZERO()
        }
    }

val AssetType.tokenId: EthUInt256?
    get() {
        return when (this) {
            is Erc721AssetType -> tokenId
            is Erc1155AssetType -> tokenId
            is Erc1155LazyAssetType -> tokenId
            is Erc721LazyAssetType -> tokenId
            is CryptoPunksAssetType -> tokenId
            is GenerativeArtAssetType, is EthAssetType, is Erc20AssetType, is CollectionAssetType, is AmmNftAssetType -> null
        }
    }

val AssetType.itemId: ItemId
    get() {
        return when (this) {
            is Erc721AssetType -> ItemId(token, tokenId.value)
            is Erc1155AssetType -> ItemId(token, tokenId.value)
            is Erc1155LazyAssetType -> ItemId(token, tokenId.value)
            is Erc721LazyAssetType -> ItemId(token, tokenId.value)
            is CryptoPunksAssetType -> ItemId(token, tokenId.value)
            is GenerativeArtAssetType,
            is EthAssetType,
            is Erc20AssetType,
            is CollectionAssetType,
            is AmmNftAssetType -> ItemId(token, BigInteger.ZERO)
        }
    }

val Order.isMakeFillOrder: Boolean
    get() = data.isMakeFillOrder(sell = make.type.nft)

val Order.token: Address
    get() = when {
        make.type.nft -> make.type.token
        take.type.nft -> take.type.token
        else -> throw IllegalArgumentException("Can't determine nft toke for order $this")
    }

val Order.currency: AssetType
    get() = when {
        make.type.nft -> take.type
        else -> make.type
    }

/**
 * All on-chain CryptoPunks orders have salt = 0.
 * We can't have a better salt for them, because there is nothing like "nonce"
 * in the CryptoPunksMarket contract.
 * Order key hash for a CryptoPunk order is as usual: Order.hashKey(maker, make.type, take.type, salt = 0)
 * So, to correctly handle orders from the same owner, we support order re-opening:
 * OrderReduceService sorts the events by timestamp and resets 'cancelled' and 'fill' fields when
 * an `OnChainOrder` event is met.
 */
val CRYPTO_PUNKS_SALT: EthUInt256 = EthUInt256.ZERO

internal const val OFFER_ITEM_TYPE_STRING =
    "OfferItem(uint8 itemType,address token,uint256 identifierOrCriteria,uint256 startAmount,uint256 endAmount)";
internal const val CONSIDERATION_ITEM_TYPE_STRING =
    "ConsiderationItem(uint8 itemType,address token,uint256 identifierOrCriteria,uint256 startAmount,uint256 endAmount,address recipient)";
internal const val ORDER_COMPONENTS_PARTIAL_TYPE_STRING =
    "OrderComponents(address offerer,address zone,OfferItem[] offer,ConsiderationItem[] consideration,uint8 orderType,uint256 startTime,uint256 endTime,bytes32 zoneHash,uint256 salt,bytes32 conduitKey,uint256 counter)";
internal const val ORDER_TYPE_STRING =
    "${ORDER_COMPONENTS_PARTIAL_TYPE_STRING}${CONSIDERATION_ITEM_TYPE_STRING}${OFFER_ITEM_TYPE_STRING}"
internal const val BLUR_ORDER_TYPE_HASH_STRING =
    "Order(address trader,uint8 side,address matchingPolicy,address collection,uint256 tokenId,uint256 amount,address paymentToken,uint256 price,uint256 listingTime,uint256 expirationTime,Fee[] fees,uint256 salt,bytes extraParams,uint256 nonce)Fee(uint16 rate,address recipient)"
internal const val BLUR_FEE_TYPE_HASH_STRING =
    "Fee(uint16 rate,address recipient)"

internal val OFFER_ITEM_TYPE_HASH = keccak256(OFFER_ITEM_TYPE_STRING.toByteArray(StandardCharsets.UTF_8))
internal val CONSIDERATION_ITEM_TYPE_HASH = keccak256(
    CONSIDERATION_ITEM_TYPE_STRING.toByteArray(StandardCharsets.UTF_8)
)
internal val ORDER_TYPE_HASH = keccak256(ORDER_TYPE_STRING.toByteArray(StandardCharsets.UTF_8))
internal val BLUR_ORDER_TYPE_HASH = keccak256(BLUR_ORDER_TYPE_HASH_STRING.toByteArray(StandardCharsets.UTF_8))
internal val BLUR_FEE_TYPE_HASH = keccak256(BLUR_FEE_TYPE_HASH_STRING.toByteArray(StandardCharsets.UTF_8))

val EXPIRED_BID_STATUSES = setOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE, OrderStatus.ENDED)