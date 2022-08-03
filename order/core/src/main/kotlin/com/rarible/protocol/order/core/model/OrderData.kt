package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.order.core.misc.zeroWord
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.Transient
import scala.Tuple2
import scala.Tuple3
import scala.Tuple4
import scala.Tuple5
import scalether.domain.Address
import java.math.BigInteger

sealed class OrderData {
    abstract val version: OrderDataVersion

    fun getDataVersion(): ByteArray? = version.ethDataType?.bytes()

    abstract fun toEthereum(wrongEncode: Boolean = false): Binary
}

data class OrderDataLegacy(
    val fee: Int
) : OrderData() {

    @get:Transient
    override val version: OrderDataVersion
        get() = OrderDataVersion.LEGACY

    override fun toEthereum(wrongEncode: Boolean): Binary = Tuples.orderDataLegacyType().encode(fee.toBigInteger())
}

sealed class OrderRaribleV2Data : OrderData()

data class OrderRaribleV2DataV1(
    val payouts: List<Part>,
    val originFees: List<Part>
) : OrderRaribleV2Data() {

    @get:Transient
    override val version: OrderDataVersion
        get() = OrderDataVersion.RARIBLE_V2_DATA_V1

    override fun toEthereum(wrongEncode: Boolean): Binary = if (wrongEncode) {
        Tuples.wrongOrderDataV1Type().encode(
            Tuple2(
                payouts.map { it.toEthereum() }.toTypedArray(),
                originFees.map { it.toEthereum() }.toTypedArray()
            )
        )
    } else {
        Tuples.orderDataV1Type().encode(
            Tuple2(
                payouts.map { it.toEthereum() }.toTypedArray(),
                originFees.map { it.toEthereum() }.toTypedArray()
            )
        )
    }
}

data class OrderRaribleV2DataV2(
    val payouts: List<Part>,
    val originFees: List<Part>,
    val isMakeFill: Boolean
) : OrderRaribleV2Data() {

    @get:Transient
    override val version: OrderDataVersion
        get() = OrderDataVersion.RARIBLE_V2_DATA_V2

    override fun toEthereum(wrongEncode: Boolean): Binary {
        val tuple3 = Tuple3(
            payouts.map { it.toEthereum() }.toTypedArray(),
            originFees.map { it.toEthereum() }.toTypedArray(),
            if (isMakeFill) BigInteger.ONE else BigInteger.ZERO
        )
        return Tuples.orderDataV2Type().encode(tuple3)
    }
}

sealed class OrderRaribleV2DataV3 : OrderRaribleV2Data() {
    abstract val payout: Part?
    abstract val originFeeFirst: Part?
    abstract val originFeeSecond: Part?
    abstract val marketplaceMarker: Word?
}

data class OrderRaribleV2DataV3Sell(
    override val payout: Part?,
    override val originFeeFirst: Part?,
    override val originFeeSecond: Part?,
    val maxFeesBasePoint: EthUInt256,
    override val marketplaceMarker: Word?
) : OrderRaribleV2DataV3() {

    @get:Transient
    override val version: OrderDataVersion
        get() = OrderDataVersion.RARIBLE_V2_DATA_V3_SELL

    override fun toEthereum(wrongEncode: Boolean): Binary {
        val tuple5 = Tuple5(
            payout?.toBigInteger() ?: BigInteger.ZERO,
            originFeeFirst?.toBigInteger() ?: BigInteger.ZERO,
            originFeeSecond?.toBigInteger() ?: BigInteger.ZERO,
            maxFeesBasePoint.value,
            (marketplaceMarker ?: zeroWord()).bytes()
        )
        return Tuples.orderDataV3SellType().encode(tuple5)
    }
}

data class OrderRaribleV2DataV3Buy(
    override val payout: Part?,
    override val originFeeFirst: Part?,
    override val originFeeSecond: Part?,
    override val marketplaceMarker: Word?
) : OrderRaribleV2DataV3() {

    @get:Transient
    override val version: OrderDataVersion
        get() = OrderDataVersion.RARIBLE_V2_DATA_V3_BUY

    override fun toEthereum(wrongEncode: Boolean): Binary {
        val tuple4 = Tuple4(
            payout?.toBigInteger() ?: BigInteger.ZERO,
            originFeeFirst?.toBigInteger() ?: BigInteger.ZERO,
            originFeeSecond?.toBigInteger() ?: BigInteger.ZERO,
            (marketplaceMarker ?: zeroWord()).bytes()
        )
        return Tuples.orderDataV3BuyType().encode(tuple4)
    }
}

data class OrderOpenSeaV1DataV1(
    val exchange: Address,
    val makerRelayerFee: BigInteger,
    val takerRelayerFee: BigInteger,
    val makerProtocolFee: BigInteger,
    val takerProtocolFee: BigInteger,
    val feeRecipient: Address,
    val feeMethod: OpenSeaOrderFeeMethod,
    val side: OpenSeaOrderSide,
    val saleKind: OpenSeaOrderSaleKind,
    val howToCall: OpenSeaOrderHowToCall,
    val callData: Binary,
    val replacementPattern: Binary,
    val staticTarget: Address,
    val staticExtraData: Binary,
    val extra: BigInteger,
    val target: Address?,
    val nonce: Long?
) : OrderData() {
    @get:Transient
    override val version = OrderDataVersion.OPEN_SEA_V1_DATA_V1

    override fun toEthereum(wrongEncode: Boolean): Binary = Binary.empty()
}

sealed class OrderSeaportDataV1 : OrderData() {
    abstract val protocol: Address
    abstract val orderType: SeaportOrderType
    abstract val offer: List<SeaportOffer>
    abstract val consideration: List<SeaportConsideration>
    abstract val zone: Address
    abstract val zoneHash: Word
    abstract val conduitKey: Word
    abstract val counter: Long
}

data class OrderBasicSeaportDataV1(
    override val protocol: Address,
    override val orderType: SeaportOrderType,
    override val offer: List<SeaportOffer>,
    override val consideration: List<SeaportConsideration>,
    override val zone: Address,
    override val zoneHash: Word,
    override val conduitKey: Word,
    override val counter: Long
) : OrderSeaportDataV1() {
    @get:Transient
    override val version = OrderDataVersion.BASIC_SEAPORT_DATA_V1

    override fun toEthereum(wrongEncode: Boolean): Binary = Binary.empty()
}

data class OrderX2Y2DataV1(
    val itemHash: Word,
    val isCollectionOffer: Boolean,
    val isBundle: Boolean,
    val side: Int,
    val orderId: BigInteger
): OrderData() {

    @get:Transient
    override val version: OrderDataVersion
        get() = OrderDataVersion.X2Y2

    override fun toEthereum(wrongEncode: Boolean): Binary = Binary.empty()
}

object OrderCryptoPunksData : OrderData() {
    @get:Transient
    override val version get() = OrderDataVersion.CRYPTO_PUNKS
    override fun toEthereum(wrongEncode: Boolean): Binary = Binary.empty()

    // We need these to easily compare JSON-deserialized objects.
    override fun equals(other: Any?): Boolean = other is OrderCryptoPunksData
    override fun hashCode() = "OrderCryptoPunksData".hashCode()
}

data class OrderLooksrareDataV1(
    val minPercentageToAsk: Int,
    val strategy: Address,
    val nonce: Long,
    val params: Binary?
): OrderData() {
    @get:Transient
    override val version get() = OrderDataVersion.LOOKSRARE_V1
    override fun toEthereum(wrongEncode: Boolean): Binary = Binary.empty()
}

enum class OrderDataVersion(val ethDataType: Binary? = null) {
    LEGACY,
    RARIBLE_V2_DATA_V1(id("V1")),
    RARIBLE_V2_DATA_V2(id("V2")),
    RARIBLE_V2_DATA_V3_SELL(id("V3_SELL")),
    RARIBLE_V2_DATA_V3_BUY(id("V3_BUY")),
    OPEN_SEA_V1_DATA_V1(id("OPEN_SEA_V1")),
    BASIC_SEAPORT_DATA_V1(id("BASIC_SEAPORT_DATA_V1")),
    CRYPTO_PUNKS,
    LOOKSRARE_V1,
    X2Y2
}

/**
 * If `true`, the [Order.fill] applies to the [Order.take] side, otherwise to the [Order.make] side.
 *
 * Historically, all Rarible V2 orders were by take side.
 * Later DataV2 was introduced with "isMakeFill" flag to support the fill by make.
 */
val OrderData.isMakeFillOrder: Boolean
    get() = (this is OrderRaribleV2DataV3Sell) || (this is OrderRaribleV2DataV2 && this.isMakeFill)

fun OrderData.getMarketplaceMarker() = if (this is OrderRaribleV2DataV3) this.marketplaceMarker else null

fun OrderData.getOriginFees(): List<Part>? {
    return when (this) {
        is OrderRaribleV2DataV1 -> this.originFees
        is OrderRaribleV2DataV2 -> this.originFees
        is OrderRaribleV2DataV3 -> listOfNotNull(this.originFeeFirst, this.originFeeSecond)
        is OrderOpenSeaV1DataV1,
        is OrderBasicSeaportDataV1,
        is OrderCryptoPunksData,
        is OrderDataLegacy,
        is OrderX2Y2DataV1,
        is OrderLooksrareDataV1 -> null
    }
}
