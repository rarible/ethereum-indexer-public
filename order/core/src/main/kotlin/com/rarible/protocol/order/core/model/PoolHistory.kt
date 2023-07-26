package com.rarible.protocol.order.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = PoolCreate::class, name = "POOL_CREAT"),
    JsonSubTypes.Type(value = PoolTargetNftOut::class, name = "POOL_NFT_OUT"),
    JsonSubTypes.Type(value = PoolTargetNftIn::class, name = "POOL_NFT_IN"),
    JsonSubTypes.Type(value = PoolNftWithdraw::class, name = "POOL_NFT_WITHDRAW"),
    JsonSubTypes.Type(value = PoolNftDeposit::class, name = "POOL_NFT_DEPOSIT"),
    JsonSubTypes.Type(value = PoolSpotPriceUpdate::class, name = "POOL_SPOT_PRICE_UPDATE"),
    JsonSubTypes.Type(value = PoolDeltaUpdate::class, name = "POOL_DELTA_UPDATE"),
    JsonSubTypes.Type(value = PoolFeeUpdate::class, name = "POOL_FEE_UPDATE"),
)
sealed class PoolHistory(var type: PoolHistoryType) : OrderHistory {
    abstract val date: Instant
    abstract val source: HistorySource
}

sealed class PoolNftChange(type: PoolHistoryType) : PoolHistory(type) {
    abstract val collection: Address
    abstract val tokenIds: List<EthUInt256>
}

sealed class PoolNftOut(type: PoolHistoryType) : PoolNftChange(type)
sealed class PoolNftIn(type: PoolHistoryType) : PoolNftChange(type)
sealed class PoolDataUpdate(type: PoolHistoryType) : PoolHistory(type)

data class PoolCreate(
    override val hash: Word,
    override val date: Instant,
    override val source: HistorySource,
    override val collection: Address,
    override val tokenIds: List<EthUInt256>,
    val currency: Address,
    val currencyBalance: BigInteger,
    val data: PoolData,
) : PoolNftIn(PoolHistoryType.POOL_CREAT) {

    fun nftAsset(): Asset {
        return Asset(
            type = AmmNftAssetType(collection),
            value = EthUInt256.ONE
        )
    }

    fun currencyAsset(): Asset {
        return Asset(
            type = if (currency == Address.ZERO()) EthAssetType else Erc20AssetType(currency),
            value = EthUInt256.ZERO
        )
    }
}

data class PoolTargetNftOut(
    override val hash: Word,
    override val collection: Address,
    override val tokenIds: List<EthUInt256>,
    override val date: Instant,
    override val source: HistorySource,
    val priceUsd: BigDecimal? = null,
    val recipient: Address,
    val outputValue: EthUInt256 = EthUInt256.ZERO,
    val marketplaceMarker: Word? = null,
) : PoolNftOut(PoolHistoryType.POOL_NFT_OUT) {

    companion object {
        fun addMarketplaceMarker(change: PoolTargetNftOut, input: Bytes, counter: RegisteredCounter? = null): PoolTargetNftOut {
            if (input.length() < 32) return change
            val lastBytes = input.bytes().takeLast(32)
            val marketplaceMarker = lastBytes
                .takeIf { it.takeLast(8) == OrderSideMatch.MARKER }
                ?.toByteArray()
                ?.let { Word.apply(it) }

            val shouldSetMarker = change.marketplaceMarker == null && marketplaceMarker != null
            return if (shouldSetMarker) {
                counter?.increment()
                change.copy(marketplaceMarker = marketplaceMarker)
            } else {
                change
            }
        }
    }
}

data class PoolNftWithdraw(
    override val hash: Word,
    override val collection: Address,
    override val tokenIds: List<EthUInt256>,
    override val date: Instant,
    override val source: HistorySource,
) : PoolNftOut(PoolHistoryType.POOL_NFT_WITHDRAW)

data class PoolTargetNftIn(
    override val hash: Word,
    override val tokenIds: List<EthUInt256>,
    override val collection: Address,
    override val date: Instant,
    override val source: HistorySource,
    val priceUsd: BigDecimal? = null,
    val tokenRecipient: Address,
    val inputValue: EthUInt256 = EthUInt256.ZERO,
) : PoolNftIn(PoolHistoryType.POOL_NFT_IN)

data class PoolNftDeposit(
    override val hash: Word,
    override val collection: Address,
    override val tokenIds: List<EthUInt256>,
    override val date: Instant,
    override val source: HistorySource,
) : PoolNftIn(PoolHistoryType.POOL_NFT_DEPOSIT)

data class PoolSpotPriceUpdate(
    override val hash: Word,
    override val date: Instant,
    override val source: HistorySource,
    val newSpotPrice: BigInteger,
) : PoolDataUpdate(PoolHistoryType.POOL_SPOT_PRICE_UPDATE)

data class PoolDeltaUpdate(
    override val hash: Word,
    override val date: Instant,
    override val source: HistorySource,
    val newDelta: BigInteger,
) : PoolDataUpdate(PoolHistoryType.POOL_DELTA_UPDATE)

data class PoolFeeUpdate(
    override val hash: Word,
    override val date: Instant,
    override val source: HistorySource,
    val newFee: BigInteger,
) : PoolDataUpdate(PoolHistoryType.POOL_FEE_UPDATE)
