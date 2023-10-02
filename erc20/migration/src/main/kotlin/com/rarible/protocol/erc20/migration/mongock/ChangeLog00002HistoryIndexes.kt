package com.rarible.protocol.erc20.migration.mongock

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.repository.Erc20ApprovalHistoryRepository
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.index.Index

@ChangeLog(order = "00002")
class ChangeLog00002HistoryIndexes {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(id = "ChangeLog00002HistoryIndexes.historyIndex001", order = "1", author = "protocol", runAlways = true)
    fun createHistoryIndexes(@NonLockGuarded template: MongoOperations) {
        listOf(Erc20TransferHistoryRepository.COLLECTION, Erc20ApprovalHistoryRepository.COLLECTION).map {
            val indexOps = template.indexOps(it)
            indexOps.ensureIndex(
                Index()
                    .on("${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::token.name}", Sort.Direction.ASC)
                    .on("${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::owner.name}", Sort.Direction.ASC)
                    .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
                    .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
                    .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.ASC)
                    .background()
            )
            indexOps.ensureIndex(
                Index()
                    .on("${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::token.name}", Sort.Direction.ASC)
                    .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
                    .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
                    .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.ASC)
                    .background()
            )
            indexOps.ensureIndex(
                Index()
                    .on("${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::owner.name}", Sort.Direction.ASC)
                    .background()
            )
        }
    }

    @ChangeSet(id = "ChangeLog00002HistoryIndexes.dropIndexes", order = "2", author = "protocol", runAlways = true)
    fun dropUnneededIndexes(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking {
        listOf(
            "data.token_1_blockNumber_1_logIndex_1",
            "data.token_1_data.owner_1_blockNumber_1_logIndex_1",
            "blockNumber_1_topic_1",
            "blockNumber_1_logIndex_1"
        ).forEach { indexName ->
            dropIndex(
                template,
                Erc20TransferHistoryRepository.COLLECTION,
                indexName
            )
        }
    }

    @ChangeSet(
        id = "ChangeLog00002HistoryIndexes.createBalanceIndexes",
        order = "3",
        author = "protocol",
        runAlways = true
    )
    fun createBalanceIndexes(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking {
        template.indexOps("erc20_balance").ensureIndex(
            Index()
                .on(Erc20Balance::owner.name, Sort.Direction.ASC)
                .background()
        )
    }

    private suspend fun dropIndex(template: ReactiveMongoOperations, collection: String, indexName: String) {
        val exists = template.indexOps(collection).indexInfo.any { it.name == indexName }.awaitFirst()

        if (!exists) {
            logger.warn("We want to drop index $collection.$indexName but it wasn't found. Skipping ...")
            return
        }
        template.indexOps(collection).dropIndex(indexName).awaitFirstOrNull()
    }
}
