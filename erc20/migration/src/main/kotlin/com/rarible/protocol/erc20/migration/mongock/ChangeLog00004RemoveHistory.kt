package com.rarible.protocol.erc20.migration.mongock

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.blockchain.scanner.ethereum.migration.ChangeLog00001
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index

@ChangeLog(order = "00004")
class ChangeLog00004RemoveHistory {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(
        id = "ChangeLog00004RemoveHistory.dropCollection",
        order = "1",
        author = "protocol",
        runAlways = false
    )
    fun dropCollection(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking {
        template.dropCollection(Erc20TransferHistoryRepository.COLLECTION).awaitFirstOrNull()
        logger.info("Collection ${Erc20TransferHistoryRepository.COLLECTION} has been dropped!")
    }

    @ChangeSet(
        id = "ChangeLog00004RemoveHistory.createCollection",
        order = "2",
        author = "protocol",
        runAlways = false
    )
    fun createCollection(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        template.createCollection(Erc20TransferHistoryRepository.COLLECTION).awaitFirstOrNull()
        val indexOps = template.indexOps(Erc20TransferHistoryRepository.COLLECTION)
        listOf(

            // blockHash
            Index()
                .on("blockHash", Sort.Direction.ASC)
                .named("blockHash")
                .background(),

            // blockNumber_1_logIndex_1
            Index()
                .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
                .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
                .background(),

            // blockNumber_1_topic_1
            Index()
                .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
                .on(ReversedEthereumLogRecord::topic.name, Sort.Direction.ASC)
                .background(),

            // data.owner_1
            Index()
                .on("${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::owner.name}", Sort.Direction.ASC)
                .background(),

            // data.token_1_blockNumber_1_logIndex_1_minorLogIndex_1
            Index()
                .on("${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::token.name}", Sort.Direction.ASC)
                .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
                .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
                .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.ASC)
                .background(),

            // data.token_1_data.owner_1_blockNumber_1_logIndex_1_minorLogIndex_1
            Index()
                .on("${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::token.name}", Sort.Direction.ASC)
                .on("${ReversedEthereumLogRecord::data.name}.${Erc20TokenHistory::owner.name}", Sort.Direction.ASC)
                .on(ReversedEthereumLogRecord::blockNumber.name, Sort.Direction.ASC)
                .on(ReversedEthereumLogRecord::logIndex.name, Sort.Direction.ASC)
                .on(ReversedEthereumLogRecord::minorLogIndex.name, Sort.Direction.ASC)
                .background(),

            // status
            Index()
                .on("status", Sort.Direction.ASC)
                .named("status")
                .background(),

            // transactionHash_1_blockHash_1_logIndex_1_minorLogIndex_1
            Index()
                .on("transactionHash", Sort.Direction.ASC)
                .on("blockHash", Sort.Direction.ASC)
                .on("logIndex", Sort.Direction.ASC)
                .on("minorLogIndex", Sort.Direction.ASC)
                .named(ChangeLog00001.UNIQUE_RECORD_INDEX_NAME)
                .background()
                .unique(),

            // transactionHash_1_topic_1_address_1_index_1_minorLogIndex_1_visible_1
            Index()
                .on("transactionHash", Sort.Direction.ASC)
                .on("topic", Sort.Direction.ASC)
                .on("address", Sort.Direction.ASC)
                .on("index", Sort.Direction.ASC)
                .on("minorLogIndex", Sort.Direction.ASC)
                .on("visible", Sort.Direction.ASC)
                .named(ChangeLog00001.VISIBLE_INDEX_NAME)
                .background()
                .unique()

        ).forEach {
            indexOps.ensureIndex(it).awaitFirstOrNull()
        }
        logger.info("All indexes have been rebuilt!")
    }
}
