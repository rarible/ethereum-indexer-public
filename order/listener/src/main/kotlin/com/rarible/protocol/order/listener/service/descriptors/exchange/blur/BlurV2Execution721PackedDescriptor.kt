package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.protocol.contracts.exchange.blur.exchange.v2.Execution721PackedEvent
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.parser.BlurV2Parser
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.blur.BlurV2EventConverter
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@EnableBlurV2
class BlurV2Execution721PackedDescriptor(
    contractsProvider: ContractsProvider,
    private val blurV2EventConverter: BlurV2EventConverter,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderSideMatch>(
    name = "blur_721_packed",
    topic = Execution721PackedEvent.id(),
    contracts = contractsProvider.blurV2(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Instant,
        index: Int,
        totalLogs: Int
    ): List<OrderSideMatch> {
        return blurV2EventConverter.convertBlurV2ExecutionEvent(
            log = log,
            transaction = transaction,
            index = index,
            totalLogs = totalLogs,
            date = timestamp
        ) {
            val event = Execution721PackedEvent.apply(log)
            BlurV2Parser.parse(event)
        }
    }
}
