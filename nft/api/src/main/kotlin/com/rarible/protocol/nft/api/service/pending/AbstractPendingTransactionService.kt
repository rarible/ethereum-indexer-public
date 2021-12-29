package com.rarible.protocol.nft.api.service.pending

import com.rarible.blockchain.scanner.ethereum.model.EthereumDescriptor
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.service.EthereumLogService
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.ethereum.log.domain.TransactionDto
import com.rarible.protocol.nft.core.model.HistoryTopics
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.service.EntityEventListener
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import scalether.abi.Signature
import scalether.domain.Address

abstract class AbstractPendingTransactionService(
    private val entityEventListeners: List<EntityEventListener>,
    private val ethereumLogService: EthereumLogService,
    private val historyTopics: HistoryTopics
) : PendingTransactionService {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun process(tx: TransactionDto): List<ReversedEthereumLogRecord> {
        val id = tx.input.slice(0, 4)
        val data = tx.input.slice(4, tx.input.length())
        val records = process(tx.hash, tx.from, tx.nonce, tx.to, id, data)
            .map { saveOrReturn(it) }
            .toList()

        val events = records.map { LogRecordEvent(it, reverted = false) }
        entityEventListeners.forEach { listener -> listener.onEntityEvents(events) }
        return records
    }

    private suspend fun saveOrReturn(logRecord: ReversedEthereumLogRecord): ReversedEthereumLogRecord {
        return try {
            ethereumLogService.save(
                EthereumDescriptor(
                    ethTopic = logRecord.topic,
                    groupId = SubscriberGroups.ITEM_HISTORY,
                    collection = requireNotNull(historyTopics[logRecord.topic]) { "Can't find collection for topic ${logRecord.topic}" },
                    contracts = emptyList(),
                    entityType = ReversedEthereumLogRecord::class.java
                ),
                listOf(logRecord)
            ).single() as ReversedEthereumLogRecord
        } catch (e: DuplicateKeyException) {
            logger.warn("history already created")
            logRecord
        }
    }

    protected abstract suspend fun process(
        hash: Word,
        from: Address,
        nonce: Long,
        to: Address?,
        id: Binary,
        data: Binary
    ) : List<ReversedEthereumLogRecord>

    protected fun <I> checkTx(id: Binary, data: Binary, signature: Signature<I, *>): I? {
        return if (id == signature.id()) {
            signature.`in`().decode(data, 0).value()
        } else {
            null
        }
    }
}