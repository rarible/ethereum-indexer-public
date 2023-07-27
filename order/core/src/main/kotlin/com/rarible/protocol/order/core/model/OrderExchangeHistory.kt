package com.rarible.protocol.order.core.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.gemswap.v1.GemSwapV1
import com.rarible.protocol.contracts.exchange.wrapper.ExchangeWrapper
import com.rarible.protocol.order.core.misc.methodSignatureId
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigDecimal
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = OrderSideMatch::class, name = "ORDER_SIDE_MATCH"),
    JsonSubTypes.Type(value = OrderCancel::class, name = "CANCEL"),
    JsonSubTypes.Type(value = OnChainOrder::class, name = "ON_CHAIN_ORDER"),
)
sealed class OrderExchangeHistory(var type: ItemType) : OrderHistory {
    abstract val make: Asset?
    abstract val take: Asset?
    abstract val date: Instant
    abstract val maker: Address?
    abstract val source: HistorySource

    @JsonIgnore
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
    val origin: Binary? = null,
    val externalOrderExecutedOnRarible: Boolean? = null,
    val data: OrderData? = null,
    val adhoc: Boolean? = null,
    val counterAdhoc: Boolean? = null,
    // Fees which were got above price
    val originFees: List<Part>? = emptyList(),
    val marketplaceMarker: Word? = null,
    val counterMarketplaceMarker: Word? = null,
    val ignoredEvent: Boolean? = null,
) : OrderExchangeHistory(type = ItemType.ORDER_SIDE_MATCH) {
    companion object {
        val MARKER = listOf<Byte>(9, 97, 108, 108, 100, 97, 116, 97)
        val MARKER_BYTES: Bytes = Binary.apply(MARKER.toByteArray())

        /**
         * Checks if marketplace marker is present in tx input and adds it to OrderSideMatch event if it's adhoc
         * Doesn't add marker if it's already present in the event
         */
        fun addMarketplaceMarker(list: List<OrderSideMatch>, input: Bytes, counter: RegisteredCounter? = null): List<OrderSideMatch> {
            if (input.length() < 32) return list
            val lastBytes = input.bytes().takeLast(32)
            val marketplaceMarker = lastBytes
                .takeIf { it.takeLast(8) == MARKER }
                ?.toByteArray()
                ?.let { Word.apply(it) }
            return list.map {
                val shouldSetMarker = it.marketplaceMarker == null && marketplaceMarker != null
                if (shouldSetMarker && it.adhoc == true) {
                    counter?.increment()
                    it.copy(marketplaceMarker = marketplaceMarker)
                } else if (shouldSetMarker && it.counterAdhoc == true) {
                    it.copy(counterMarketplaceMarker = marketplaceMarker)
                } else {
                    it
                }
            }
        }

        fun getRealTaker(originTaker: Address, transaction: Transaction): Address {
            return when (transaction.input().methodSignatureId()) {
                GemSwapV1.batchBuyWithETHSignature().id(),
                GemSwapV1.batchBuyWithERC20sSignature().id(),
                ExchangeWrapper.singlePurchaseSignature().id(),
                ExchangeWrapper.bulkPurchaseSignature().id(),
                ExchangeWrapper.bulkPurchaseSignature().id() -> transaction.from()
                else -> originTaker
            }
        }
    }
}

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
    override val source: HistorySource = platform.toHistorySource()
) : OrderExchangeHistory(ItemType.ON_CHAIN_ORDER)
