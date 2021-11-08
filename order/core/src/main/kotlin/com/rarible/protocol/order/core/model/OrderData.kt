package com.rarible.protocol.order.core.model

import com.rarible.protocol.contracts.Tuples
import io.daonomic.rpc.domain.Binary
import org.springframework.data.annotation.Transient
import scala.Tuple2
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

data class OrderRaribleV2DataV1(
    val payouts: List<Part>,
    val originFees: List<Part>
) : OrderData() {

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
    val extra: BigInteger
) : OrderData() {
    @get:Transient
    override val version = OrderDataVersion.OPEN_SEA_V1_DATA_V1

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

enum class OrderDataVersion(val ethDataType: Binary? = null) {
    LEGACY,
    RARIBLE_V2_DATA_V1(id("V1")),
    OPEN_SEA_V1_DATA_V1(id("OPEN_SEA_V1")),
    CRYPTO_PUNKS
}

