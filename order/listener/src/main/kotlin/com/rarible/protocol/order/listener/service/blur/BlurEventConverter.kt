package com.rarible.protocol.order.listener.service.blur

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.blur.v1.BlurV1
import com.rarible.protocol.contracts.exchange.blur.v1.OrderCancelledEvent
import com.rarible.protocol.contracts.exchange.blur.v1.OrdersMatchedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.BlurOrder
import com.rarible.protocol.order.core.model.BlurOrderSide
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.parser.BlurOrderParser
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.converter.AbstractEventConverter
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Component
class BlurEventConverter(
    traceCallService: TraceCallService,
    featureFlags: OrderIndexerProperties.FeatureFlags,
    private val standardProvider: TokenStandardProvider,
) : AbstractEventConverter(traceCallService, featureFlags) {

    suspend fun convert(
        event: OrdersMatchedEvent,
        date: Instant,
        input: Bytes,
    ): List<OrderSideMatch> {
        TODO()
    }

    suspend fun convert(
        log: Log,
        transaction: Transaction,
        index: Int,
        totalLogs: Int,
        date: Instant
    ): List<OrderCancel> {
        val event = OrderCancelledEvent.apply(log)
        val inputs = getMethodInput(
            event.log(),
            transaction,
            BlurV1.cancelOrderSignature().id(), BlurV1.cancelOrderSignature().id()
        )
        require(inputs.size == totalLogs) {
            "Canceled orders in tx ${transaction.hash()} didn't match total events, inputs=${inputs.size}, totalLogs=$totalLogs"
        }
        return BlurOrderParser.parserOrder(inputs[index]).map {
            val nft = getNftAsset(it)
            val currency = getCurrencyAsset(it)
            val (make, take) = when (it.side) {
                BlurOrderSide.SELL -> nft to currency
                BlurOrderSide.BUY ->  currency to nft
            }
            OrderCancel(
                hash = Word.apply(event.hash()),
                maker = it.trader,
                make = make,
                take = take,
                date = date,
                source = HistorySource.BLUR
            )
        }
    }

    private suspend fun getNftAsset(order: BlurOrder): Asset {
        val collection = order.collection
        val tokenId = EthUInt256.of(order.tokenId)
        val value = EthUInt256.of(order.amount)
        return when (standardProvider.getTokenStandard(collection)) {
            TokenStandard.ERC1155 -> Erc1155AssetType(collection, tokenId)
            TokenStandard.ERC721 -> Erc721AssetType(collection, tokenId)
            null -> throw IllegalArgumentException("Invalid token standard for $collection")
        }.let { Asset(it, value) }
    }

    private suspend fun getCurrencyAsset(order: BlurOrder): Asset {
        return when (val paymentToken = order.paymentToken) {
            Address.ZERO() -> EthAssetType
            else -> Erc20AssetType(paymentToken)
        }.let { Asset(it, EthUInt256.of(order.price)) }
    }
}