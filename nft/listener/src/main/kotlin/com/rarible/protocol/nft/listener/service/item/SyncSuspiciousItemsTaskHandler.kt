package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.kafka.chunked
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.misc.nftTaskEventMarks
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceEventListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SyncSuspiciousItemsTaskHandler(
    private val itemExStateRepository: ItemExStateRepository,
    private val itemRepository: ItemRepository,
    private val eventListener: ItemReduceEventListener
) : TaskHandler<String> {

    override val type: String
        get() = SYNC_SUSPICIOUS_ITEMS

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val fromItemId = from?.let { ItemId.parseId(from) }
        return itemExStateRepository
            .getAll(fromItemId)
            .chunked(CHUNK_SIZE)
            .map { chunk ->
                itemRepository
                    .searchByIds(chunk.map { it.id }.toSet())
                    .map { item ->
                        val eventTimeMarks = nftTaskEventMarks()
                        logger.info("Suspicious item event: itemId={}, suspicious={}", item.id, item.isSuspiciousOnOS)
                        eventListener.onItemChanged(item, eventTimeMarks).awaitFirstOrNull()
                        item.id.stringValue
                    }
                    .last()
            }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SyncSuspiciousItemsTaskHandler::class.java)
        private const val SYNC_SUSPICIOUS_ITEMS = "SYNC_SUSPICIOUS_ITEMS"
        private const val CHUNK_SIZE = 500
    }
}
