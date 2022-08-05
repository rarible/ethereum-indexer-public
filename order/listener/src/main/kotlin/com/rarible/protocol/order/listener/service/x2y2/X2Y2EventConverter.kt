package com.rarible.protocol.order.listener.service.x2y2

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
import java.math.BigInteger
import java.time.Instant
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class X2Y2EventConverter(
    private val orderRepository: OrderRepository,
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
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

    suspend fun convert(event: EvInventoryEvent, date: Instant): List<OrderSideMatch> {
        if (event.detail()._1() != BigInteger.ONE) return emptyList()
        val maker = event.maker()
        val taker = event.taker()
        val tokenData = Tuples.addressUintType().decode(Binary(event.item()._2), 0).value()
        val make = Asset(
            type = Erc721AssetType(
                token = tokenData._1,
                tokenId = EthUInt256(tokenData._2)
            ),
            value = EthUInt256.ONE
        )
        val currency = event.currency()
        val take = Asset(
            type = if (currency == Address.ZERO()) {
                EthAssetType
            } else Erc20AssetType(token = currency),
            value = EthUInt256(event.item()._1)
        )
        val leftUsdValue = priceUpdateService.getAssetsUsdValue(make = make, take = take, at = date)
        val rightUsdValue = priceUpdateService.getAssetsUsdValue(make = take, take = make, at = date)

        val fee = event.detail()._11().map {
            Part(account = it._2, value = EthUInt256(it._1))
        }
        val hash = Word.apply(event.itemHash())
        val counterHash = keccak256(hash)
        return listOf(
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
                originFees = fee,
                adhoc = true,
                counterAdhoc = false
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
                originFees = fee,
                adhoc = false,
                counterAdhoc = true
            )
        )
    }
}
