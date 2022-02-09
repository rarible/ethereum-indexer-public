package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.Task
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.model.ReduceTokenRangeItemsTaskParams
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.listener.service.item.ItemReduceState
import com.rarible.protocol.nft.listener.service.item.ItemReduceTaskHandler
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ReduceTokenRangeTaskHandler(
    private val itemReduceService: ItemReduceService,
    private val taskRepository: TaskRepository
) : TaskHandler<ItemReduceState> {

    override val type: String
        get() = ITEM_RANGE_REDUCE

    override suspend fun isAbleToRun(param: String): Boolean {
        return verifyAllTopicCompleted(ItemType.TRANSFER.topic + ItemType.ROYALTY.topic) && verifyReduceCompleted()
    }

    override fun runLongTask(from: ItemReduceState?, param: String): Flow<ItemReduceState> {
        val taskParam = ReduceTokenRangeItemsTaskParams.fromParamString(param)

        val taskFrom = from?.let { ItemId(it.token, it.tokenId) } ?: taskParam.from.let { ItemId(it, MAX_TOKEN_ID) }
        val taskTo = taskParam.to

        return itemReduceService.update(from = taskFrom, to = taskTo)
            .map { (token, tokenId) -> ItemReduceState(token, tokenId) }
            .windowTimeout(Int.MAX_VALUE, Duration.ofSeconds(5))
            .flatMap {
                it.next()
            }
            .asFlow()
    }

    private suspend fun verifyAllTopicCompleted(topics: Iterable<Word>): Boolean {
        for (topic in topics) {
            val task = findTopicTask(topic)
            if (task?.lastStatus != TaskStatus.COMPLETED) {
                return false
            }
        }
        return true
    }

    private suspend fun verifyReduceCompleted(): Boolean {
        val task = findReduceTask()
        if (task?.lastStatus != TaskStatus.COMPLETED) {
            return false
        }
        return true
    }

    private suspend fun findTopicTask(topic: Word): Task? {
        return taskRepository.findByTypeAndParam(ReindexTopicTaskHandler.TOPIC, topic.toString()).awaitFirstOrNull()
    }

    private suspend fun findReduceTask(): Task? {
        return taskRepository.findByTypeAndParam(ItemReduceTaskHandler.ITEM_REDUCE, "").awaitFirstOrNull()
    }

    companion object {
        val MAX_TOKEN_ID = EthUInt256.of("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
        const val ITEM_RANGE_REDUCE = "ITEM_RANGE_REDUCE"
    }
}