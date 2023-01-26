package com.rarible.protocol.order.core.service.block.approval

import com.rarible.contracts.erc721.ApprovalForAllEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.EVENT)
@Deprecated("Should be removed after switch to the new scanner")
class ApprovalAllLogListener(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val approveService: ApproveService,
    private val properties: OrderIndexerProperties
) : OnLogEventListener {

    override val topics: List<Word>
        get() = listOf(ApprovalForAllEvent.id())

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono { reduceOrders(logEvent) }.then()

    override fun onRevertedLogEvent(logEvent: LogEvent): Mono<Void> = mono { reduceOrders(logEvent) }.then()

    private suspend fun reduceOrders(logEvent: LogEvent) {
        val history = logEvent.data as ApprovalHistory
        val blockNumber = logEvent.blockNumber ?: error("blockTimestamp can't be null")
        if (properties.handleApprovalAfterBlock > blockNumber) {
            logger.info(
                "Skip approval reindex event: block={}, tc={}, logIndex={}",
                logEvent.blockNumber, logEvent.transactionHash, logEvent.logIndex
            )
            return
        }
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
                orderUpdateService.updateApproval(
                    it,
                    history.approved,
                    blockchainEventMark(logEvent.blockNumber).source
                )
            }
    }
}
