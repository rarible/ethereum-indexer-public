package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.order.logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component

@Component
class CancelSuspiciousItemOrdersTaskHandler(
    private val nftItemApi: NftItemControllerApi,
    private val orderItemService: OrderItemService
) : TaskHandler<String> {

    override val type = "CANCEL_SUSPICIOUS_ITEM_ORDERS"

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        var cursor = from
        do {
            val page = nftItemApi.getNftAllItems(
                cursor,
                200,
                true,
                null,
                null
            ).awaitFirst()

            cursor = page.continuation
            val suspicious = page.items.filter { it.isSuspiciousOnOS == true }
            suspicious.forEach { orderItemService.onItemChanged(it, orderTaskEventMarks()) }

            logger.info("Found {} suspicious Items in batch of {}", suspicious.size, page.items.size)

            cursor?.let { emit(it) }
        } while (cursor != null)
    }
}
