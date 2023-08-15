package com.rarible.protocol.order.core.service.looksrare

import com.rarible.looksrare.client.LooksrareClientV2
import com.rarible.looksrare.client.model.LooksrareError
import com.rarible.looksrare.client.model.LooksrareResult
import com.rarible.looksrare.client.model.OperationResult
import com.rarible.looksrare.client.model.v2.EventsRequest
import com.rarible.looksrare.client.model.v2.LooksrareEvent
import com.rarible.looksrare.client.model.v2.LooksrareEventType
import com.rarible.looksrare.client.model.v2.LooksrareObject
import com.rarible.looksrare.client.model.v2.LooksrareOrder
import com.rarible.looksrare.client.model.v2.LooksrareResponse
import com.rarible.looksrare.client.model.v2.OrdersRequest
import com.rarible.looksrare.client.model.v2.Pagination
import com.rarible.looksrare.client.model.v2.QuoteType
import com.rarible.looksrare.client.model.v2.Sort
import com.rarible.looksrare.client.model.v2.Status
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.misc.looksrareInfo
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.isSell
import com.rarible.protocol.order.core.model.nft
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.model.tokenId
import com.rarible.protocol.order.core.service.OrderStateCheckService
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Component
class LooksrareService(
    private val looksrareClient: LooksrareClientV2,
    private val properties: LooksrareLoadProperties,
) : OrderStateCheckService {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getNextSellOrders(cursor: LooksrareV2Cursor): List<LooksrareOrder> =
        load(cursor = cursor, type = "orders") { nextId ->
            val request = OrdersRequest(
                quoteType = QuoteType.ASK,
                status = Status.VALID,
                sort = Sort.NEWEST,
                pagination = Pagination(first = properties.loadMaxSize, cursor = nextId)
            )
            ({
                looksrareClient.getOrders(request)
            })
        }

    suspend fun getNextCancelListEvents(cursor: LooksrareV2Cursor): List<LooksrareEvent> =
        load(cursor = cursor, type = "cancel_list events") { nextId ->
            val request = EventsRequest(
                type = LooksrareEventType.CANCEL_LIST,
                pagination = Pagination(first = properties.loadMaxSize, cursor = nextId)
            )
            ({
                looksrareClient.getEvents(request)
            })
        }

    private suspend fun <T : LooksrareObject> load(
        cursor: LooksrareV2Cursor,
        type: String,
        call: suspend (nextId: String?) -> suspend () -> LooksrareResult<LooksrareResponse<T>>
    ): List<T> {
        val data = ConcurrentLinkedQueue<T>()
        val createdAfter = cursor.createdAfter
        val nextId = AtomicReference(cursor.nextId)
        val deep = AtomicInteger()
        do {
            val request = call(nextId.get())

            val result = requestWithRetry(type = type, call = request)
            data.addAll(result.data)

            val lastItem = result.data.lastOrNull()
            logger.looksrareInfo(
                "Load next: createdAfter=$createdAfter, cursor=$nextId, last=${lastItem?.createdAt}, deep=$deep"
            )
            nextId.set(lastItem?.id)
        } while (
            lastItem != null &&
            lastItem.createdAt > createdAfter &&
            deep.incrementAndGet() < properties.loadMaxDeep
        )

        return data.toSet().toList()
    }

    private suspend fun <T> requestWithRetry(
        type: String,
        call: suspend () -> LooksrareResult<LooksrareResponse<T>>
    ): LooksrareResponse<T> {
        val lastError = AtomicReference<LooksrareError>()
        val retries = AtomicInteger()

        while (retries.get() < properties.retry) {
            when (val result = call()) {
                is OperationResult.Success -> return result.result
                is OperationResult.Fail -> lastError.set(result.error)
            }
            retries.incrementAndGet()
            delay(properties.retryDelay)
        }
        throw IllegalStateException("Can't fetch Looksrare $type, number of attempts exceeded, last error: $lastError")
    }

    override suspend fun isActiveOrder(order: Order): Boolean {
        val nft = order.nft()
        val tokenId = nft.type.tokenId?.value?.toString()
            ?: throw IllegalStateException("Can't get tokenId for order: ${order.hash}")
        val nextId = AtomicReference<String>()
        repeat(properties.loadMaxDeep) {
            val orders = requestWithRetry(type = "orders") {
                looksrareClient.getOrders(
                    OrdersRequest(
                        collection = nft.type.token,
                        itemId = tokenId,
                        quoteType = if (order.isSell()) QuoteType.ASK else QuoteType.BID,
                        status = Status.VALID,
                        sort = Sort.NEWEST,
                        pagination = Pagination(first = properties.loadMaxSize, cursor = nextId.get())
                    )
                )
            }.data
            if (orders.any { it.hash == order.hash }) {
                return true
            }
            if (orders.isEmpty()) {
                return false
            }
            nextId.set(orders.last().id)
        }
        return true
    }
}
