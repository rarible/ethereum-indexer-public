package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.order.core.model.PoolHistoryType
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Component

@Component
class PoolReduceTaskHandler(
    private val taskRepository: TaskRepository,
    private val orderReduceService: OrderReduceService,
    private val poolHistory: PoolHistoryRepository
) : TaskHandler<String> {

    override val type: String
        get() = POOL_REDUCE

    override suspend fun isAbleToRun(param: String): Boolean =
        verifyAllReindexingTasksCompleted(PoolHistoryType.values().flatMap { it.topic })

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return poolHistory.findDistinctHashes(from = from?.let { Word.apply(it) }).asFlux()
            .flatMap {
                orderReduceService.update(orderHash = it)
            }
            .filter { it.hash != OrderReduceService.EMPTY_ORDER_HASH }
            .map { it.hash.toString() }
            .asFlow()
    }

    private suspend fun verifyAllReindexingTasksCompleted(topics: Iterable<Word>): Boolean =
        topics.all { isTaskCompleted(ReindexTopicTaskHandler.TOPIC, it.toString()) }

    private suspend fun isTaskCompleted(type: String, param: String): Boolean {
        val task = taskRepository.findByTypeAndParam(type, param).awaitFirstOrNull()
        return task == null || task.lastStatus == TaskStatus.COMPLETED
    }

    companion object {
        const val POOL_REDUCE = "POOL_REDUCE"
    }
}