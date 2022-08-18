package com.rarible.protocol.order.listener.service.descriptors.approval

import com.rarible.contracts.erc721.ApprovalForAllEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.EVENT)
class ApprovalAllLogListener(
    private val orderRepository: OrderRepository,
    private val orderReduceService: OrderReduceService
) : OnLogEventListener {
    override val topics: List<Word>
        get() = listOf(ApprovalForAllEvent.id())

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        reduceOrders(logEvent.data as ApprovalHistory)
    }.then()

    override fun onRevertedLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        reduceOrders(logEvent.data as ApprovalHistory)
    }.then()

    private suspend fun reduceOrders(history: ApprovalHistory) {
        orderRepository.findActiveSaleOrdersHashesByMakerAndToken(maker = history.owner, token = history.collection).collect {
            orderReduceService.updateOrder(it)
        }
    }
}
