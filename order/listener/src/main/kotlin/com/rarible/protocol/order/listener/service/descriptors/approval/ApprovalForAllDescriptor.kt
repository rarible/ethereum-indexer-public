package com.rarible.protocol.order.listener.service.descriptors.approval

import com.rarible.contracts.erc721.ApprovalForAllEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.contracts.ApprovalForAllByTopicsEvent
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.listener.service.descriptors.ApprovalSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ApprovalForAllDescriptor(
    private val approveService: ApproveService
) : ApprovalSubscriber(
    name = "approval",
    topic = ApprovalForAllEvent.id(),
    contracts = emptyList()
) {
    override suspend fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Instant,
        index: Int,
        totalLogs: Int
    ): List<ApprovalHistory> {
        val eventData = convert(log)
        return if (eventData.operator in approveService.operators) {
            listOf(eventData)
        } else emptyList()
    }

    private fun convert(log: Log): ApprovalHistory {
        val event = when(log.topics().size()) {
            4 -> ApprovalForAllByTopicsEvent.apply(log)
            else -> ApprovalForAllEvent.apply(log)
        }
        return ApprovalHistory(
            collection = log.address(),
            owner = event.owner(),
            operator = event.operator(),
            approved = event.approved()
        )
    }
}
