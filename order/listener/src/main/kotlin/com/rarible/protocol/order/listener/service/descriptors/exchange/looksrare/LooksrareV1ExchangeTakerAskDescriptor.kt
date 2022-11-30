package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.looksrare.v1.TakerAskEvent
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.service.descriptors.ContractsProvider
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import scalether.domain.response.Log

@Service
@CaptureSpan(type = SpanType.EVENT)
class LooksrareV1ExchangeTakerAskDescriptor(
    contractsProvider: ContractsProvider,
    orderRepository: OrderRepository,
    looksrareCancelOrdersEventMetric: RegisteredCounter,
    looksrareTakeAskEventMetric: RegisteredCounter,
    wrapperLooksrareMatchEventMetric: RegisteredCounter,
    tokenStandardProvider: TokenStandardProvider,
    priceUpdateService: PriceUpdateService,
    prizeNormalizer: PriceNormalizer,
) : AbstractLooksrareV1ExchangeTakerDescriptor(
    TakerAskEvent.id(),
    contractsProvider,
    orderRepository,
    looksrareCancelOrdersEventMetric,
    looksrareTakeAskEventMetric,
    wrapperLooksrareMatchEventMetric,
    tokenStandardProvider,
    priceUpdateService,
    prizeNormalizer,
) {
    override fun getTakeEvent(log: Log): TakeEvent {
        val event = TakerAskEvent.apply(log)
        return TakeEvent(
            taker = event.taker(),
            maker = event.maker(),
            orderHash = Word.apply(event.orderHash()),
            orderNonce = event.orderNonce().toLong(),
            currency = event.currency(),
            collection = event.collection(),
            tokenId = EthUInt256.of(event.tokenId()),
            amount = EthUInt256.of(event.amount()),
            price = EthUInt256.of(event.price()),
            isAsk = false
        )
    }
}
