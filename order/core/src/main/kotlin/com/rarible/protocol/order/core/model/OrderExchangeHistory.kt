package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

sealed class OrderExchangeHistory(var type: ItemType) : EventData {
    abstract val hash: Word
    abstract val make: Asset?
    abstract val take: Asset?
    abstract val date: Instant
    abstract val maker: Address?
    abstract val source: HistorySource

    fun isBid() = take?.type?.nft ?: false
}

enum class OrderSide {
    LEFT,
    RIGHT
}

data class OrderSideMatch(
    override val hash: Word,
    val counterHash: Word? = null,
    val side: OrderSide?,
    val fill: EthUInt256,
    override val maker: Address,
    val taker: Address,
    override val make: Asset,
    override val take: Asset,
    val makeUsd: BigDecimal?,
    val takeUsd: BigDecimal?,
    val makePriceUsd: BigDecimal?,
    val takePriceUsd: BigDecimal?,
    val makeValue: BigDecimal?,
    val takeValue: BigDecimal?,
    override val date: Instant = nowMillis(),
    override val source: HistorySource = HistorySource.RARIBLE,
    val externalOrderExecutedOnRarible: Boolean? = null,
    val data: OrderData? = null,
    val adhoc: Boolean? = null,
    val counterAdhoc: Boolean? = null
) : OrderExchangeHistory(type = ItemType.ORDER_SIDE_MATCH)

data class OrderCancel(
    override val hash: Word,
    override val maker: Address?,
    override val make: Asset?,
    override val take: Asset?,
    override val date: Instant = nowMillis(),
    override val source: HistorySource = HistorySource.RARIBLE
) : OrderExchangeHistory(type = ItemType.CANCEL)

data class OnChainOrder(
    override val maker: Address,
    val taker: Address?,
    override val make: Asset,
    override val take: Asset,
    val createdAt: Instant,
    val platform: Platform,
    val orderType: OrderType,
    val salt: EthUInt256,
    val start: Long?,
    val end: Long?,
    val data: OrderData,
    val signature: Binary?,
    val priceUsd: BigDecimal?,
    override val hash: Word,
    override val date: Instant = createdAt,
    override val source: HistorySource = when (platform) {
        Platform.RARIBLE -> HistorySource.RARIBLE
        Platform.OPEN_SEA -> HistorySource.OPEN_SEA
        Platform.CRYPTO_PUNKS -> HistorySource.CRYPTO_PUNKS
    }
) : OrderExchangeHistory(ItemType.ON_CHAIN_ORDER)
