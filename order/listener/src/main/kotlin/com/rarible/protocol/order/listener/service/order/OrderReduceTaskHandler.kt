package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.service.OrderReduceService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
class OrderReduceTaskHandler(
    private val taskRepository: TaskRepository,
    private val orderReduceService: OrderReduceService
) : TaskHandler<String> {

    override val type: String
        get() = ORDER_REDUCE

    override suspend fun isAbleToRun(param: String): Boolean =
        isTaskCompleted(OnChainOrderVersionInsertionTaskHandler.TYPE, "", completedIfNotExists = false)
                && verifyAllReindexingTasksCompleted(ItemType.values().flatMap { it.topic })

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return orderReduceService.update(null, fromOrderHash = from?.let { Word.apply(it) })
            .map { it.hash.toString() }
            .asFlow()
    }

    private suspend fun verifyAllReindexingTasksCompleted(topics: Iterable<Word>): Boolean =
        topics.all { isTaskCompleted(ReindexTopicTaskHandler.TOPIC, it.toString()) }

    private suspend fun isTaskCompleted(type: String, param: String, completedIfNotExists: Boolean = true): Boolean {
        val task = taskRepository.findByTypeAndParam(type, param).awaitFirstOrNull()
        return (task == null && completedIfNotExists) || task?.lastStatus == TaskStatus.COMPLETED
    }

    companion object {
        const val ORDER_REDUCE = "ORDER_REDUCE"
    }
}
