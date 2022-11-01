package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.ifNotBlank
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.Order.Id.Companion.toOrderId
import com.rarible.protocol.order.core.model.Platform
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
        verifyAllReindexingTasksCompleted(ItemType.values().flatMap { it.topic })

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val platforms = param
            .ifNotBlank()
            ?.split(",")
            ?.map { Platform.valueOf(it) }
            ?.takeUnless { it.isEmpty() }

        return orderReduceService.update(
            fromOrderHash = from?.toOrderId()?.hash,
            platforms = platforms,
            orderHash = null,
        )
            .filter { it.hash != OrderReduceService.EMPTY_ORDER_HASH }
            .map { it.id.toString() }
            .asFlow()
    }

    private suspend fun verifyAllReindexingTasksCompleted(topics: Iterable<Word>): Boolean =
        topics.all { isTaskCompleted(ReindexTopicTaskHandler.TOPIC, it.toString()) }

    private suspend fun isTaskCompleted(type: String, param: String): Boolean {
        val task = taskRepository.findByTypeAndParam(type, param).awaitFirstOrNull()
        return task == null || task.lastStatus == TaskStatus.COMPLETED
    }

    companion object {
        const val ORDER_REDUCE = "ORDER_REDUCE"
    }
}
