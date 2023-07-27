package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceTokenRangeTaskParams
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component

@Component
class ReduceTokenRangeTaskHandler(
    private val itemReduceService: ItemReduceService
) : TaskHandler<String> {

    override val type = ReduceTokenRangeTaskParams.ADMIN_REDUCE_TOKEN_RANGE

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val itemId = from?.let { ItemId.parseId(it) }
        val params = ReduceTokenRangeTaskParams.parse(param)
        val fromItemId = itemId ?: ItemId.parseId(params.from)
        val toItemId = ItemId.parseId(params.to)

        return itemReduceService
            .update(from = fromItemId, to = toItemId, updateNotChanged = false)
            .map { it.stringValue }
            .asFlow()
    }
}
