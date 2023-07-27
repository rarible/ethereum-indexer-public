package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepositoryIndexes
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "00021")
class ChangeLog00021ApprovalHistoryIndexes {

    @ChangeSet(
        id = "ChangeLog00021ApprovalIndexes.createApprovalHistoryRepositoryIndexes",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForAll(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        ApprovalHistoryRepositoryIndexes.ALL_INDEXES.forEach { index ->
            template.indexOps(ApprovalHistoryRepository.COLLECTION).ensureIndex(index).awaitFirst()
        }
    }
}
