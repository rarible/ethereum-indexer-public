package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.ReindexTokenTaskParams
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ReduceTokenTaskHandler(
    private val taskRepository: TempTaskRepository,
    private val itemReduceService: ItemReduceService
) : TaskHandler<String> {

    override val type: String
        get() = ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS

    override suspend fun isAbleToRun(param: String): Boolean {
        val taskParams = ReduceTokenItemsTaskParams.fromParamString(param)
        return verifyAllReindexTokenTaskCompleted(taskParams)
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val params = ReduceTokenItemsTaskParams.fromParamString(param)
        val itemId = from?.let { ItemId.parseId(it) }

        return itemReduceService
            .update(token = params.token, tokenId = null, from = itemId)
            .map { it.stringValue }
            .asFlow()
    }

    private suspend fun verifyAllReindexTokenTaskCompleted(params: ReduceTokenItemsTaskParams): Boolean {
        return params.token !in findReindexingTokens()
    }

    private suspend fun findReindexingTokens(): List<Address> {
        return taskRepository
            .findByType(ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .map { ReindexTokenTaskParams.fromParamString(it.param).tokens }
            .toList()
            .flatten()
    }
}
