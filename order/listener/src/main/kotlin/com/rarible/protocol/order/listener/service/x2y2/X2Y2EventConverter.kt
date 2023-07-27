package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.common.keccak256
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.exchange.x2y2.v1.EvCancelEvent
import com.rarible.protocol.contracts.x2y2.v1.events.EvInventoryEvent
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.time.Instant

@Component
class X2Y2EventConverter(
    private val orderRepository: OrderRepository,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val wrapperX2Y2MatchEventMetric: RegisteredCounter,
) {
    suspend fun convert(event: EvCancelEvent, date: Instant): OrderCancel {
            val hash = Word.apply(event.itemHash())
            val order = orderRepository.findById(hash)
            return OrderCancel(
                hash = hash,
                maker = order?.maker,
                make = order?.make,
                take = order?.take,
                date = date,
                source = HistorySource.X2Y2
            )
    }

    suspend fun convert(
        event: EvInventoryEvent,
        date: Instant,
        transaction: Transaction
    ): List<OrderSideMatch> {
        val input = transaction.input()
        val op = event.detail()._1().toInt()
        if ((op == 1 || op == 2).not()) return emptyList()

        val maker = event.maker()
        val taker = OrderSideMatch.getRealTaker(event.taker(), transaction)
        val tokenData = Tuples.addressUintType().decode(Binary(event.item()._2), 64).value()

        val nft = Asset(
            type = Erc721AssetType(
                token = tokenData._1,
                tokenId = EthUInt256(tokenData._2)
            ),
            value = EthUInt256.ONE
        )
        val currency = Asset(
            type = if (event.currency() == Address.ZERO()) {
                EthAssetType
            } else Erc20AssetType(token = event.currency()),
            value = EthUInt256(event.item()._1)
        )
        val fee = event.detail()._11().map {
            Part(account = it._2, value = EthUInt256(it._1))
        }
        val (make, take) = when (op) {
            /**
             Op {
                INVALID, (0)
                // off-chain
                COMPLETE_SELL_OFFER, (1)
                COMPLETE_BUY_OFFER, (2)
                CANCEL_OFFER,
                // auction
                BID,
                COMPLETE_AUCTION,
                REFUND_AUCTION,
                REFUND_AUCTION_STUCK_ITEM
             }
             */
            1 -> nft to currency // Sell
            2 -> currency to nft // Bid
            else -> throw UnsupportedOperationException("Unsupported operation")
        }
        val leftUsdValue = priceUpdateService.getAssetsUsdValue(make = make, take = take, at = date)
        val rightUsdValue = priceUpdateService.getAssetsUsdValue(make = take, take = make, at = date)

        val hash = Word.apply(event.itemHash())
        val counterHash = keccak256(hash)

        val events = listOf(
            OrderSideMatch(
                hash = hash,
                counterHash = counterHash,
                side = OrderSide.LEFT,
                maker = maker,
                taker = taker,
                make = make,
                take = take,
                fill = make.value,
                makePriceUsd = leftUsdValue?.makePriceUsd,
                takePriceUsd = leftUsdValue?.takePriceUsd,
                makeUsd = leftUsdValue?.makeUsd,
                takeUsd = leftUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(make),
                takeValue = prizeNormalizer.normalize(take),
                source = HistorySource.X2Y2,
                date = date,
                originFees = fee,
                adhoc = false,
                counterAdhoc = true,
            ),
            OrderSideMatch(
                hash = counterHash,
                counterHash = hash,
                side = OrderSide.RIGHT,
                maker = taker,
                taker = maker,
                make = take,
                take = make,
                fill = take.value,
                makePriceUsd = rightUsdValue?.makePriceUsd,
                takePriceUsd = rightUsdValue?.takePriceUsd,
                makeUsd = rightUsdValue?.makeUsd,
                takeUsd = rightUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(take),
                takeValue = prizeNormalizer.normalize(make),
                source = HistorySource.X2Y2,
                date = date,
                originFees = fee,
                adhoc = true,
                counterAdhoc = false
            )
        )
        return OrderSideMatch.addMarketplaceMarker(events, input, wrapperX2Y2MatchEventMetric)
    }
}
