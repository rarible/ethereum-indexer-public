package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.Task
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
        verifyAllCompleted(ItemType.values().flatMap { it.topic })

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return orderReduceService.update(null, fromOrderHash = from?.let { Word.apply(it) })
            .map { it.order.hash.toString() }
            .asFlow()
    }

    private suspend fun verifyAllCompleted(topics: Iterable<Word>): Boolean {
        for (topic in topics) {
            val task = findTask(topic)
            if (task != null && task.lastStatus != TaskStatus.COMPLETED) {
                return false
            }
        }
        return true
    }

    private suspend fun findTask(topic: Word): Task? {
        return taskRepository.findByTypeAndParam(ReindexTopicTaskHandler.TOPIC, topic.toString()).awaitFirstOrNull()
    }

    companion object {
        const val ORDER_REDUCE = "ORDER_REDUCE"
    }
}
