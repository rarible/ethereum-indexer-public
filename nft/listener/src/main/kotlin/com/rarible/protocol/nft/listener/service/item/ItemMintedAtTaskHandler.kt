package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.misc.div
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ItemMintedAtTaskHandler(
    private val itemRepository: ItemRepository,
    private val mongo: ReactiveMongoOperations
) : TaskHandler<String> {

    override val type: String
        get() = MINTED_AT


    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("", null))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val query = Query(
            Criteria().andOperator(
                LogEvent::data / ItemTransfer::type isEqualTo ItemType.TRANSFER,
                LogEvent::status isEqualTo LogEventStatus.CONFIRMED,
                LogEvent::data / ItemTransfer::from isEqualTo Address.ZERO(),
                from?.let { LogEvent::id gt ObjectId(it) } ?: Criteria()
        )).with(Sort.by(Sort.Direction.ASC, LogEvent::id.name))
        return mongo.find(query, LogEvent::class.java, NftItemHistoryRepository.COLLECTION).asFlow()
            .onEach(this::processLogEvent)
            .map { it.id.toString() }
    }

    suspend fun processLogEvent(logEvent: LogEvent) {
        val data = logEvent.data
        when (data) {
            is ItemTransfer -> {
                val itemId = ItemId(data.token, data.tokenId)
                val item = itemRepository.findById(itemId).awaitSingle()
                itemRepository.save(item.copy(mintedAt = data.date)).awaitFirstOrNull()
                logger.info("Save item $itemId with mintedAt date: ${data.date}")
            }
            else -> IllegalArgumentException("LogEvent with id=${logEvent.id} is not Transfer Event")
        }
    }

    companion object {
        const val MINTED_AT = "MINTED_AT"
        private val logger = LoggerFactory.getLogger(ItemMintedAtTaskHandler::class.java)
    }
}
