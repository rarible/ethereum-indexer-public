package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ReduceEventListenerListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SyncSuspiciousItemsTaskHandler(
    private val itemExStateRepository: ItemExStateRepository,
    private val itemRepository: ItemRepository,
    private val eventListener: ReduceEventListenerListener
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
            .mapNotNull {
                itemRepository.findById(it.id).awaitFirstOrNull()
            }
            .map {
                logger.info("Send sync suspicious event: itemId={}, suspicious={}", it.id, it.isSuspiciousOnOS)
                eventListener.onItemChanged(it).awaitFirstOrNull()
                it.id.stringValue
            }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SyncSuspiciousItemsTaskHandler::class.java)
        const val SYNC_SUSPICIOUS_ITEMS = "SYNC_SUSPICIOUS_ITEMS"
    }
}