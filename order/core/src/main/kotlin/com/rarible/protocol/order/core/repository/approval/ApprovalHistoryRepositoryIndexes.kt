package com.rarible.protocol.order.core.repository.approval

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.model.ApprovalHistory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

object ApprovalHistoryRepositoryIndexes {

    private val LAST_APPROVAL_EVENT = Index()
        .on("${LogEvent::data.name}.${ApprovalHistory::collection.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ApprovalHistory::owner.name}", Sort.Direction.ASC)
        .on(LogEvent::blockNumber.name, Sort.Direction.DESC)
        .on(LogEvent::logIndex.name, Sort.Direction.DESC)
        .on(LogEvent::minorLogIndex.name, Sort.Direction.DESC)
        .background()

    val ALL_INDEXES = listOf(
        LAST_APPROVAL_EVENT
    )

}
