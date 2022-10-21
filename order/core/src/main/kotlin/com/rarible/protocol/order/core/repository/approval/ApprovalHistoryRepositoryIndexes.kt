package com.rarible.protocol.order.core.repository.approval

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.model.ApprovalHistory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

object ApprovalHistoryRepositoryIndexes {

    private val LAST_APPROVAL_EVENT = Index()
        .on("${LogEvent::data.name}.${ApprovalHistory::collection.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ApprovalHistory::owner.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ApprovalHistory::operator.name}", Sort.Direction.ASC)
        .on(LogEvent::blockNumber.name, Sort.Direction.ASC)
        .on(LogEvent::logIndex.name, Sort.Direction.ASC)
        .on(LogEvent::minorLogIndex.name, Sort.Direction.ASC)
        .background()

    @Deprecated("Need remove")
    private val LAST_APPROVAL_EVENT_LEGACY = Index()
        .on("${LogEvent::data.name}.${ApprovalHistory::collection.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ApprovalHistory::owner.name}", Sort.Direction.ASC)
        .on(LogEvent::blockNumber.name, Sort.Direction.DESC)
        .on(LogEvent::logIndex.name, Sort.Direction.DESC)
        .on(LogEvent::minorLogIndex.name, Sort.Direction.DESC)
        .background()

    val ALL_INDEXES = listOf(
        LAST_APPROVAL_EVENT,
    )

}
