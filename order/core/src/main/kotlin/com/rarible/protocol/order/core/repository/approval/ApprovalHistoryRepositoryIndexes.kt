package com.rarible.protocol.order.core.repository.approval

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.protocol.order.core.model.ApprovalHistory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

object ApprovalHistoryRepositoryIndexes {

    private val LAST_APPROVAL_EVENT = Index()
        .on("${ReversedEthereumLogRecord::data.name}.${ApprovalHistory::collection.name}", Sort.Direction.ASC)
        .on("${ReversedEthereumLogRecord::data.name}.${ApprovalHistory::owner.name}", Sort.Direction.ASC)
        .on("${ReversedEthereumLogRecord::data.name}.${ApprovalHistory::operator.name}", Sort.Direction.ASC)
        .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
        .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
        .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.ASC)
        .background()

    @Deprecated("Need remove")
    private val LAST_APPROVAL_EVENT_LEGACY = Index()
        .on("${ReversedEthereumLogRecord::data.name}.${ApprovalHistory::collection.name}", Sort.Direction.ASC)
        .on("${ReversedEthereumLogRecord::data.name}.${ApprovalHistory::owner.name}", Sort.Direction.ASC)
        .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.DESC)
        .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.DESC)
        .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.DESC)
        .background()

    val ALL_INDEXES = listOf(
        LAST_APPROVAL_EVENT,
    )
}
