package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@ChangeLog(order = "00026")
class ChangeLog00026RemoveUnusedIndexes {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(
        id = "ChangeLog00026RemoveUnusedIndexes", order = "1", author = "protocol"
    )
    fun removeIndexes(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking<Unit> {

        dropIndex(template, MongoOrderRepository.COLLECTION, "createdAt_1__id_1")
        dropIndex(template, MongoOrderRepository.COLLECTION, "make.type.nft_1_make.type.token_1_status_1_lastUpdateAt_1__id_1")

        dropIndex(template, ApprovalHistoryRepository.COLLECTION, "transactionHash_1_blockHash_1_logIndex_1_minorLogIndex_1")

        dropIndex(template, ExchangeHistoryRepository.COLLECTION, "data.hash_1_data.source_1_blockNumber_1_logIndex_1_minorLogIndex_1")
    }

    private suspend fun dropIndex(template: ReactiveMongoOperations, collection: String, indexName: String) {
        val exists = template.indexOps(collection).indexInfo.any { it.name == indexName }.awaitFirst()

        if (!exists) {
            logger.warn("We want to drop index $collection.$indexName but it now found. Skipping ...")
            return
        }

        template.indexOps(collection).dropIndex(indexName).awaitFirstOrNull()
    }
}
