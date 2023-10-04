package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.protocol.contracts.exchange.blur.v1.OrderCancelledEvent
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.blur.BlurEventConverter
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@EnableBlur
class BlurV1ExchangeCancelDescriptor(
    contractsProvider: ContractsProvider,
    private val blurEventConverter: BlurEventConverter,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderCancel>(
    name = "blur_cancelled",
    topic = OrderCancelledEvent.id(),
    contracts = contractsProvider.blurV1(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        return blurEventConverter.convertToCancel(log, transaction, index, totalLogs, timestamp)
    }
}
