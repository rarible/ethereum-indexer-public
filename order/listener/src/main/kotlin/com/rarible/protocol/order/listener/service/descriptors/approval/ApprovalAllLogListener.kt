package com.rarible.protocol.order.listener.service.descriptors.approval

import com.rarible.contracts.erc721.ApprovalForAllEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

@Component
@CaptureSpan(type = SpanType.EVENT)
class ApprovalAllLogListener(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val approveService: ApproveService,
    private val properties: OrderListenerProperties
) : OnLogEventListener {

    override val topics: List<Word>
        get() = listOf(ApprovalForAllEvent.id())

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono { reduceOrders(logEvent) }.then()

    override fun onRevertedLogEvent(logEvent: LogEvent): Mono<Void> = mono { reduceOrders(logEvent) }.then()

    private suspend fun reduceOrders(logEvent: LogEvent) {
        val history = logEvent.data as ApprovalHistory
        val platform = approveService.getPlatform(history.operator) ?: run {
            logger.error("Can't get platform by operator ${history.operator}, event: $history")
            return
        }
        logger.info(
            "Process approval: maker={}, collection={}, platform={}, block={}, logIndex={}",
            history.owner, history.collection, platform, logEvent.blockNumber, logEvent.logIndex
        )
        orderRepository
            .findActiveSaleOrdersHashesByMakerAndToken(maker = history.owner, token = history.collection, platform)
            .collect {
                randomDelay()
                orderUpdateService.updateApproval(it, history.approved)
            }
    }

    //TODO://Need to remove this on PT-1657
    private suspend fun randomDelay() {
        val maxDelayMs = properties.approvalEvenHandleDelay.toMillis()
        if (maxDelayMs == 0L) return
        val delayMs = ThreadLocalRandom.current().nextLong(0, maxDelayMs)
        delay(timeMillis = delayMs)

    }
}
