package com.rarible.protocol.erc20.listener.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.repository.Erc20ApprovalHistoryRepository
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.index.Index

@ChangeLog(order = "00002")
class ChangeLog00002HistoryIndexes {

    @ChangeSet(id = "ChangeLog00002HistoryIndexes.historyIndex001", order = "1", author = "protocol", runAlways = true)
    fun createHistoryIndexes(@NonLockGuarded template: MongoOperations) {
        listOf(Erc20TransferHistoryRepository.COLLECTION, Erc20ApprovalHistoryRepository.COLLECTION).map {
            val indexOps = template.indexOps(it)
            indexOps.ensureIndex(
                Index()
                    .on("${LogEvent::data.name}.${Erc20TokenHistory::token.name}", Sort.Direction.ASC)
                    .on("${LogEvent::data.name}.${Erc20TokenHistory::owner.name}", Sort.Direction.ASC)
                    .on("blockNumber", Sort.Direction.ASC)
                    .on("logIndex", Sort.Direction.ASC)
            )
            indexOps.ensureIndex(
                Index()
                    .on("${LogEvent::data.name}.${Erc20TokenHistory::token.name}", Sort.Direction.ASC)
                    .on("blockNumber", Sort.Direction.ASC)
                    .on("logIndex", Sort.Direction.ASC)
            )
            indexOps.ensureIndex(
                Index()
                    .on("blockNumber", Sort.Direction.ASC)
                    .on("logIndex", Sort.Direction.ASC)
            )
        }
    }
}
