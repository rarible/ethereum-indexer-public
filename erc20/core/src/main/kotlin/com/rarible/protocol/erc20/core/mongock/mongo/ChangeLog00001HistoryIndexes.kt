package com.rarible.protocol.erc20.core.mongock.mongo

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.repository.Erc20ApprovalHistoryRepository
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.index.Index

//@ChangeLog(order = "00001")
class ChangeLog00001HistoryIndexes {

//    @ChangeSet(id = "ChangeLog00001HistoryIndexes.historyIndex001", order = "1", author = "protocol")
    fun createHistoryIndexes(/*@NonLockGuarded*/ template: MongoOperations) {
        listOf(Erc20TransferHistoryRepository.COLLECTION, Erc20ApprovalHistoryRepository.COLLECTION).map {
            val indexOps = template.indexOps(Erc20TransferHistoryRepository.COLLECTION)
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
