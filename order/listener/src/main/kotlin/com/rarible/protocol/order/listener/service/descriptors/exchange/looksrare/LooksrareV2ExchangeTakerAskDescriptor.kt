package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.looksrare.v2.TakerAskEvent
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.Address
import scalether.domain.response.Log
import java.math.BigInteger

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV2ExchangeTakerAskDescriptor(
    contractsProvider: ContractsProvider,
    orderRepository: OrderRepository,
    looksrareCancelOrdersEventMetric: RegisteredCounter,
    looksrareTakeAskEventMetric: RegisteredCounter,
    wrapperLooksrareMatchEventMetric: RegisteredCounter,
    tokenStandardProvider: TokenStandardProvider,
    priceUpdateService: PriceUpdateService,
    prizeNormalizer: PriceNormalizer,
) : AbstractLooksrareV1ExchangeTakerDescriptor(
    name = "lr_v2_taker_ask",
    TakerAskEvent.id(),
    contractsProvider.looksrareV2(),
    contractsProvider,
    orderRepository,
    looksrareCancelOrdersEventMetric,
    looksrareTakeAskEventMetric,
    wrapperLooksrareMatchEventMetric,
    tokenStandardProvider,
    priceUpdateService,
    prizeNormalizer,
) {
    override fun getTakeEvent(log: Log): TakeEvent? {
        val event = TakerAskEvent.apply(log)
        val tokenId = event.itemIds().takeIf { ids -> ids.size == 1 }?.single() ?: run {
            logger.warn("Item id is not single: ${event.itemIds()}, tx=${log.transactionHash()}")
            return null
        }
        val amount = event.amounts().takeIf { ids -> ids.size == 1 }?.single() ?: run {
            logger.warn("Item amount is not single: ${event.amounts()}, tx=${log.transactionHash()}")
            return null
        }
        val taker = event.bidUser()
        val maker = event.feeRecipients()[0]
        val creator = event.feeRecipients()[1]
        val price = event.feeAmounts()[0]
        val royalty = event.feeAmounts()[1]
        val fee = event.feeAmounts()[2]
        return TakeEvent(
            maker = maker,
            taker = taker,
            orderHash = Word.apply(event.nonceInvalidationParameters()._1()),
            orderNonce = event.nonceInvalidationParameters()._2(),
            currency = event.currency(),
            collection = event.collection(),
            tokenId = EthUInt256.of(tokenId),
            amount = EthUInt256.of(amount),
            price = EthUInt256.of(price),
            isAsk = true,
            royalty = if (creator == Address.ZERO() || royalty == BigInteger.ZERO) null else {
                Royalty(creator, EthUInt256.of(royalty))
            },
            protocolFee = fee,
            isNonceInvalidated = event.nonceInvalidationParameters()._3(),
        )
    }
}
