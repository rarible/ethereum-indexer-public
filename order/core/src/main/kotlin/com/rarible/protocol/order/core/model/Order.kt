package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.Tuples.keccak256
import com.rarible.protocol.order.core.misc.plus
import com.rarible.protocol.order.core.misc.zeroWord
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import scala.*
import scalether.abi.Uint256Type
import scalether.abi.Uint8Type
import scalether.domain.Address
import scalether.util.Hash
import scalether.util.Hex
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

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

    val pending: List<OrderExchangeHistory> = emptyList(),

    val makePriceUsd: BigDecimal? = null,
    val takePriceUsd: BigDecimal? = null,
    val makePrice: BigDecimal? = null,
    val takePrice: BigDecimal? = null,
    val makeUsd: BigDecimal? = null,
    val takeUsd: BigDecimal? = null,
    val priceHistory: List<OrderPriceHistoryRecord> = emptyList(),

    // TODO: this field is 'var' and calculated in the constructor to make sure it is recalculated on any Order.copy()
    //  Is there a better way of storing "computable" fields in the Mongo, but ignoring them on deserialization?
    var status: OrderStatus = OrderStatus.ACTIVE,

    val platform: Platform = Platform.RARIBLE,

    val lastEventId: String? = null,

    @Id
    val hash: Word = hashKey(maker, make.type, take.type, salt.value, data),

    /**
     * TODO: we need this field only temporarily, until the "OrderReduceService" refactoring commit is released and is proven to be stable.
     *  This is needed to not corrupt production database completely if we need to revert the above commit.
     *  That commit removes the field, and the old production release will not be able to start.
     */
    val version: Long? = 1
) {
    init {
        status = calculateStatus(fill, make, take, makeStock, cancelled, start, end, data)
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
        data.getDataVersion(),
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
                start = start,
                end = end
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

    fun withUpdatedStatus(): Order {
        return copy(status = calculateStatus(fill, make, take, makeStock, cancelled, start, end, data))
    }

    fun isEnded() = Companion.isEnded(end)

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
            start: Long?,
            end: Long?
        ): EthUInt256 {
            if (makeValue == EthUInt256.ZERO || takeValue == EthUInt256.ZERO) {
                return EthUInt256.ZERO
            }
            val (make) = calculateRemaining(makeValue, takeValue, fill, cancelled, data)
            val fee = if (feeSide == FeeSide.MAKE) calculateFee(data, protocolCommission) else EthUInt256.ZERO

            val roundedMakeBalance = calculateRoundedMakeBalance(
                makeValue = makeValue,
                takeValue = takeValue,
                makeBalance = (makeBalance * EthUInt256.of(10000)) / (fee + EthUInt256.of(10000))
            )

            val calculatedMakeStock = minOf(make, roundedMakeBalance)

            return when(orderType) {
                OrderType.RARIBLE_V2, OrderType.RARIBLE_V1, OrderType.CRYPTO_PUNKS -> calculatedMakeStock
                OrderType.OPEN_SEA_V1 -> if (make > roundedMakeBalance) EthUInt256.ZERO else calculatedMakeStock
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
            data: OrderData
        ): OrderStatus {
            return when {
                data.isMakeFillOrder && fill >= make.value -> OrderStatus.FILLED
                fill >= take.value -> OrderStatus.FILLED
                cancelled -> OrderStatus.CANCELLED
                makeStock > EthUInt256.ZERO && isAlive(start, end) -> OrderStatus.ACTIVE
                !isStarted(start) -> OrderStatus.NOT_STARTED
                isEnded(end) -> OrderStatus.ENDED
                else -> OrderStatus.INACTIVE
            }
        }

        private fun isAlive(start: Long?, end: Long?) = isStarted(start) && !isEnded(end)

        private fun isEnded(end: Long?): Boolean {
            val now = Instant.now().epochSecond
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
            data: OrderData
        ): Pair<EthUInt256, EthUInt256> {
            return if (cancelled) {
                EthUInt256.ZERO to EthUInt256.ZERO
            } else if (data.isMakeFillOrder) {
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
            return when (data) {
                is OrderRaribleV2DataV1 -> data.originFees.fold(protocolCommission) { acc, part -> acc + part.value  }
                is OrderRaribleV2DataV2 -> data.originFees.fold(protocolCommission) { acc, part -> acc + part.value  }
                is OrderDataLegacy -> EthUInt256.of(data.fee.toLong())
                is OrderOpenSeaV1DataV1 -> EthUInt256.ZERO
                is OrderCryptoPunksData -> EthUInt256.ZERO
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
                OrderType.RARIBLE_V1 -> raribleExchangeV1Hash(maker, make,  take, salt, data)
                OrderType.OPEN_SEA_V1 -> openSeaV1Hash(maker, make, taker, take, salt, start, end, data)
                OrderType.CRYPTO_PUNKS -> throw IllegalArgumentException("On-chain CryptoPunks orders are not hashable")
            }
        }

        fun Order.legacyMessage(): String {
            return legacyMessage(maker, make, take, salt.value, data)
        }

        fun OrderVersion.legacyMessage(): String {
            return legacyMessage(maker, make, take, salt.value, data)
        }

        private fun legacyMessage(maker: Address, make: Asset, take: Asset, salt: BigInteger, data: OrderData): String {
            val legacyMakeAsset = make.type.toLegacy() ?: error("Unsupported make asset ${make.type} by legacy contract")
            val legacyTakeAsset = take.type.toLegacy() ?: error("Unsupported take asset ${take.type} by legacy contract")
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
            val legacyMakeAsset = make.type.toLegacy() ?: error("Unsupported make asset ${make.type} by legacy contract")
            val legacyTakeAsset = take.type.toLegacy() ?: error("Unsupported take asset ${take.type} by legacy contract")
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
                        data.getDataVersion(),
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
            val openSeaData = (data as? OrderOpenSeaV1DataV1) ?: error("Unsupported data type ${data.javaClass} for OpenSea contract")
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
                    OPENSEA_ORDER_TYPE_HASH.bytes(),
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

        fun hashKey(
            maker: Address,
            makeAssetType: AssetType,
            takeAssetType: AssetType,
            salt: BigInteger,
            data: OrderData? = null
        ): Word = when (data) {
            is OrderRaribleV2DataV2 -> {
                // For RaribleV2 DataV2 hash contains the data bytes.
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

        private val TYPE_HASH =
            keccak256("Order(address maker,Asset makeAsset,address taker,Asset takeAsset,uint256 salt,uint256 start,uint256 end,bytes4 dataType,bytes data)Asset(AssetType assetType,uint256 value)AssetType(bytes4 assetClass,bytes data)")

        val OPENSEA_ORDER_TYPE_HASH = Word.apply("0xdba08a88a748f356e8faf8578488343eab21b1741728779c9dcfdc782bc800f8")

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
    }
}

fun Order.invert(
    maker: Address,
    amount: BigInteger,
    newSalt: Word = zeroWord(),
    newData: OrderData = this.data
): Order = run {
    val (makeValue, takeValue) = calculateAmounts(make.value.value, take.value.value, amount, isBid())
    this.copy(
        maker = maker,
        taker = this.maker,
        make = take.copy(value = EthUInt256(makeValue)),
        take = make.copy(value = EthUInt256(takeValue)),
        data = newData,
        hash = Order.hashKey(maker, take.type, make.type, salt.value, newData),
        salt = EthUInt256.of(newSalt),
        makeStock = EthUInt256.ZERO,
        signature = null,
        lastUpdateAt = nowMillis()
    )
}

fun Order.isBid() = take.type.nft

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
            is GenerativeArtAssetType, is EthAssetType, is Erc20AssetType, is CollectionAssetType -> null
        }
    }

val Order.makeNftItemId: ItemId?
    get() = make.type.tokenId?.let { ItemId(make.type.token, it.value) }

val Order.takeNftItemId: ItemId?
    get() = take.type.tokenId?.let { ItemId(take.type.token, it.value) }

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
