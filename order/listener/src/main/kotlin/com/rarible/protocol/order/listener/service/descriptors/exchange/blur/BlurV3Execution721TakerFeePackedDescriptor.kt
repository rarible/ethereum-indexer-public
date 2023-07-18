package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.protocol.contracts.exchange.blur.exchange.v2.Execution721TakerFeePackedEvent
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.parser.BlurV2Parser
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.blur.BlurEventConverter
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class BlurV3Execution721TakerFeePackedDescriptor(
    contractsProvider: ContractsProvider,
    private val blurEventConverter: BlurEventConverter,
) : ExchangeSubscriber<OrderSideMatch>(
    name = "blur_721_taker_fee_packed",
    topic = Execution721TakerFeePackedEvent.id(),
    contracts = contractsProvider.blurV2()
) {
    override suspend fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Instant,
        index: Int,
        totalLogs: Int
    ): List<OrderSideMatch> {
        return blurEventConverter.convertBlurV2ExecutionEvent(
            log = log,
            transaction = transaction,
            index = index,
            totalLogs = totalLogs,
            date = timestamp
        ) {
            val event = Execution721TakerFeePackedEvent.apply(log)
            BlurV2Parser.parse(event)
        }
    }
}

