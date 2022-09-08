package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

sealed class PoolHistory(var type: PoolHistoryType) : OrderHistory {
    abstract val date: Instant
    abstract val source: HistorySource
}

data class OnChainAmmOrder(
    override val hash: Word,
    override val date: Instant,
    override val source: HistorySource,
    val maker: Address,
    val make: Asset,
    val take: Asset,
    val data: OrderAmmData,
    val tokenIds: List<EthUInt256>,
    val price: BigInteger,
    val priceValue: BigDecimal,
    val priceUsd: BigDecimal?,
) : PoolHistory(PoolHistoryType.POOL_CREAT) {
    fun isBid() = take.type.nft
    fun isSell() = isBid().not()
}

sealed class PoolNftOut(type: PoolHistoryType) : PoolHistory(type) {
    abstract val tokenIds: List<EthUInt256>
}

data class PoolTargetNftOut(
    override val hash: Word,
    override val tokenIds: List<EthUInt256>,
    override val date: Instant,
    override val source: HistorySource,
    val recipient: Address,
) : PoolNftOut(PoolHistoryType.POOL_NFT_OUT)

data class PoolNftWithdraw(
    override val hash: Word,
    override val tokenIds: List<EthUInt256>,
    override val date: Instant,
    override val source: HistorySource,
    val collection: Address,
) : PoolNftOut(PoolHistoryType.POOL_NFT_WITHDRAW)


sealed class PoolNftIn(type: PoolHistoryType) : PoolHistory(type) {
    abstract val tokenIds: List<EthUInt256>
}

data class PoolTargetNftIn(
    override val hash: Word,
    override val tokenIds: List<EthUInt256>,
    override val date: Instant,
    override val source: HistorySource,
    val tokenRecipient: Address,
) : PoolNftIn(PoolHistoryType.POOL_NFT_IN)

data class PoolNftDeposit(
    override val hash: Word,
    override val tokenIds: List<EthUInt256>,
    override val date: Instant,
    override val source: HistorySource,
    val collection: Address,
) : PoolNftIn(PoolHistoryType.POOL_NFT_DEPOSIT)

sealed class PoolDataUpdate(type: PoolHistoryType) : PoolHistory(type)

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
