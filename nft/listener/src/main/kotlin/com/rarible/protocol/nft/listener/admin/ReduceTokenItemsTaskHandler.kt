package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.ReindexTokenItemsTaskParams
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import scalether.domain.Address

/**
 * Background job that reduces all items of a token (specified by `param`).
 * This can be run only after [ReindexTokenItemsTaskHandler] is finished for this token.
 */
@Component
class ReduceTokenItemsTaskHandler(
    private val taskRepository: TempTaskRepository,
    private val itemReduceService: ItemReduceService
) : TaskHandler<String> {

    override val type: String
        get() = ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS

    override suspend fun isAbleToRun(param: String): Boolean {
        val taskParams = ReduceTokenItemsTaskParams.fromParamString(param)
        return taskParams.oneToken !in findTokensBeingIndexedNow()
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val params = ReduceTokenItemsTaskParams.fromParamString(param)
        val itemId = from?.let { ItemId.parseId(it) }

        return itemReduceService
            .update(token = params.oneToken, tokenId = null, from = itemId)
            .map { it.stringValue }
            .asFlow()
    }

    private suspend fun findTokensBeingIndexedNow(): List<Address> {
        return taskRepository
            .findByType(ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .map { ReindexTokenItemsTaskParams.fromParamString(it.param).tokens }
            .toList()
            .flatten()
    }
}
