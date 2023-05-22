package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceTokenItemsDependentTaskParams
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

/**
 * Background job that reduces all items of a token (specified by `param`).
 * This can be run only after dependent task is finished.
 */
@Component
class ReduceTokenItemsDependentTaskHandler(
    private val listenerProps: NftListenerProperties,
    private val taskRepository: TempTaskRepository,
    private val itemReduceService: ItemReduceService
) : TaskHandler<String> {

    override val type: String
        get() = ReduceTokenItemsDependentTaskParams.REDUCE_TOKEN_ITEMS_DEPENDENT

    override suspend fun isAbleToRun(param: String): Boolean {
        val taskParams = ReduceTokenItemsDependentTaskParams.parse(param)
        return tokenIsIndexed(taskParams.dependency) && haveCapacityToRunReduce()
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val params = ReduceTokenItemsDependentTaskParams.parse(param)
        val itemId = from?.let { ItemId.parseId(it) }

        return itemReduceService
            .update(token = params.address, tokenId = null, from = itemId, updateNotChanged = false)
            .map { it.stringValue }
            .asFlow()
    }

    private suspend fun tokenIsIndexed(id: String): Boolean {
        return taskRepository.findById(ObjectId(id))?.let { it.lastStatus == TaskStatus.COMPLETED } ?: false
    }

    private suspend fun haveCapacityToRunReduce(): Boolean {
        return taskRepository.countRunningTasks(type) < listenerProps.fixStandardJob.reduceBatch
    }
}
