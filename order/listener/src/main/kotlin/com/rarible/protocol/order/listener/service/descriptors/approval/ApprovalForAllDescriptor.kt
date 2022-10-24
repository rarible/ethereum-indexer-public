package com.rarible.protocol.order.listener.service.descriptors.approval

import com.rarible.contracts.erc721.ApprovalForAllEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.ApprovalForAllByTopicsEvent
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import com.rarible.protocol.order.core.service.approve.ApproveService
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
@CaptureSpan(type = SpanType.EVENT)
class ApprovalForAllDescriptor(
    private val approveService: ApproveService
) : LogEventDescriptor<ApprovalHistory> {

    override val collection: String = ApprovalHistoryRepository.COLLECTION

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(emptyList())

    override val topic: Word = ApprovalForAllEvent.id()

    override fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Long,
        index: Int,
        totalLogs: Int
    ): Publisher<ApprovalHistory> {
        val eventData = convert(log)
        return if (eventData.operator in approveService.operators) {
            eventData.toMono()
        } else Mono.empty()
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
