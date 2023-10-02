package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.contracts.blur.v1.evemts.OrdersMatchedEvent
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.blur.BlurEventConverter
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
@EnableBlur
class BlurV1ExchangeDescriptor(
    contractsProvider: ContractsProvider,
    private val blurEventConverter: BlurEventConverter,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderSideMatch>(
    name = "blur_matched",
    topic = OrdersMatchedEvent.id(),
    contracts = contractsProvider.blurV1(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderSideMatch> {
        return blurEventConverter.convertToSideMatch(log, transaction, index, totalLogs, timestamp)
    }
}
