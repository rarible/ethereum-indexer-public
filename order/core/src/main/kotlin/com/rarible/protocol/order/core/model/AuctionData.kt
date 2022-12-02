package com.rarible.protocol.order.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import io.daonomic.rpc.domain.Binary
import org.springframework.data.annotation.Transient
import scala.Tuple5
import java.math.BigInteger
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "version")
@JsonSubTypes(
    JsonSubTypes.Type(value = RaribleAuctionV1DataV1::class, name = "RARIBLE_AUCTION_V1_DATA_V1"),
)
sealed class AuctionData {
    abstract val version: AuctionDataVersion
    abstract val startTime: Instant?

    fun getDataVersion(): ByteArray? = version.ethDataType.bytes()

    abstract fun toEthereum(): Binary

    companion object {
        fun decode(version: Binary, data: Binary): AuctionData {
            return when (version) {
                AuctionDataVersion.RARIBLE_AUCTION_V1_DATA_V1.ethDataType -> {
                    val decoded = Tuples.auctionDataV1Type().decode(data, 0)
                    RaribleAuctionV1DataV1(
                        payouts = decoded.value()._1().map { it.toPart() },
                        originFees = decoded.value()._2().map { it.toPart() },
                        duration = EthUInt256.of(decoded.value()._3()),
                        startTime = decoded.value()._4().takeUnless { it == BigInteger.ZERO }
                            ?.let { Instant.ofEpochSecond(it.toLong()) },
                        buyOutPrice = decoded.value()._5().takeUnless { it == BigInteger.ZERO }
                            ?.let { EthUInt256.of(it) }
                    )
                }
                else -> throw IllegalArgumentException("Can't parse auction data")
            }
        }
    }
}

data class RaribleAuctionV1DataV1(
    val originFees: List<Part>,
    val payouts: List<Part>,
    val duration: EthUInt256,
    override val startTime: Instant?,
    val buyOutPrice: EthUInt256?
) : AuctionData() {

    @get:Transient
    override val version: AuctionDataVersion
        get() = AuctionDataVersion.RARIBLE_AUCTION_V1_DATA_V1

    override fun toEthereum(): Binary {
        return Tuples.auctionDataV1Type().encode(
            Tuple5(
                payouts.map { it.toEthereum() }.toTypedArray(),
                originFees.map { it.toEthereum() }.toTypedArray(),
                duration.value,
                startTime?.epochSecond?.toBigInteger() ?: BigInteger.ZERO,
                buyOutPrice?.value ?: BigInteger.ZERO
            )
        )
    }
}

enum class AuctionDataVersion(val ethDataType: Binary) {
    RARIBLE_AUCTION_V1_DATA_V1(id("V1")),
}
