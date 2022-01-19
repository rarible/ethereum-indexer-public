package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.common.convert
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.nft.core.model.ExtendedItem
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.kafka.common.cache.LRUCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.Executors

/**
 * Utility class that is used to guarantee that we will eventually send ItemUpdateEvent
 * to Kafka despite anything wrong happens to the item meta loading infrastructure.
 *
 * Since the loader library is not stable yet, we can't risk of not sending ItemUpdateEvent-s.
 * So for all items for which we did not have meta available when the items were updated,
 * we schedule after a fixed delay a 100% sending of the event.
 */
@Component
class ItemUpdateEventAssertQueue(
    private val itemRepository: ItemRepository,
    private val protocolNftEventPublisher: ProtocolNftEventPublisher,
    private val conversionService: ConversionService
) : AutoCloseable {

    companion object {
        val delay: Duration = Duration.ofMinutes(5)
    }

    @Lazy
    @Autowired
    lateinit var itemMetaService: ItemMetaService

    private val logger = LoggerFactory.getLogger(ItemUpdateEventAssertQueue::class.java)

    private val daemonDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, ItemUpdateEventAssertQueue::class.simpleName).apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private val scope = CoroutineScope(SupervisorJob() + daemonDispatcher)

    private val sentItems = LRUCache<ItemId, Unit>(1024)

    fun onItemEventSent(itemId: ItemId) {
        logger.info("Event for item ${itemId.decimalStringValue} has been sent, no need to re-assert")
        synchronized(sentItems) {
            sentItems.put(itemId, Unit)
        }
    }

    fun sendItemUpdateEventAfter(itemId: ItemId, sendDelay: Duration) {
        scope.launch {
            delay(sendDelay.toMillis())
            sendItemUpdateEvent(itemId)
        }
    }

    private suspend fun sendItemUpdateEvent(itemId: ItemId) {
        val item = itemRepository.findById(itemId).awaitFirstOrNull() ?: return
        val availableMeta = itemMetaService.getAvailable(itemId)
        val wasAlreadySent = synchronized(sentItems) { sentItems.get(itemId) != null }
        logger.info(
            "Sending item update event for ${itemId.decimalStringValue} " +
                    "with " + (if (availableMeta != null) "not" else "") + " available meta" +
                    ", was already sent = $wasAlreadySent"
        )
        protocolNftEventPublisher.publish(
            conversionService.convert<NftItemEventDto>(ExtendedItem(item, availableMeta))
        )
    }

    override fun close() {
        scope.cancel()
        daemonDispatcher.close()
    }
}
