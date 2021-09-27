package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.Task
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
class OnChainOrderVersionInsertionTaskHandler(
    private val taskRepository: TaskRepository,
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderUpdateService: OrderUpdateService
) : TaskHandler<String> {

    override val type: String
        get() = TYPE

    override suspend fun isAbleToRun(param: String): Boolean =
        verifyAllReindexingTasksCompleted(ItemType.values().flatMap { it.topic })

    override fun runLongTask(from: String?, param: String): Flow<String> =
        exchangeHistoryRepository.findLogEvents(null, from = from?.let { Word.apply(it) })
            .asFlow()
            .filter { it.data is OnChainOrder }
            .onEach { orderUpdateService.saveOrRemoveOnChainOrderVersions(listOf(it)) }
            .map { (it.data as OnChainOrder).hash.toString() }

    private suspend fun verifyAllReindexingTasksCompleted(topics: Iterable<Word>): Boolean {
        for (topic in topics) {
            val task = findReindexingTask(topic)
            if (task != null && task.lastStatus != TaskStatus.COMPLETED) {
                return false
            }
        }
        return true
    }

    private suspend fun findReindexingTask(topic: Word): Task? =
        taskRepository.findByTypeAndParam(ReindexTopicTaskHandler.TOPIC, topic.toString()).awaitFirstOrNull()

    companion object {
        const val TYPE = "ON_CHAIN_ORDER_VERSION_INSERTION"
    }
}
