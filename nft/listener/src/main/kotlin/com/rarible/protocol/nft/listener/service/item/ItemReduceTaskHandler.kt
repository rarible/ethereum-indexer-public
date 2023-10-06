package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.Task
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Duration

@Component
class ItemReduceTaskHandler(
    private val itemReduceService: ItemReduceService,
    private val taskRepository: TaskRepository,
) : TaskHandler<ItemReduceState> {

    override val type: String
        get() = ITEM_REDUCE

    override fun runLongTask(from: ItemReduceState?, param: String): Flow<ItemReduceState> {
        val (fromItemId, toItemId) = rangeItemId(from, param)
        return (if (fromItemId != null && fromItemId == toItemId) {
            itemReduceService.update(
                token = fromItemId.token,
                tokenId = fromItemId.tokenId,
                updateNotChanged = false
            )
        } else {
            itemReduceService.update(
                from = fromItemId,
                to = toItemId,
                updateNotChanged = false
            )
        }).map { (token, tokenId) -> ItemReduceState(token, tokenId) }
            .windowTimeout(Int.MAX_VALUE, Duration.ofSeconds(5))
            .flatMap {
                it.takeLast(1).next()
            }
            .asFlow()
    }

    private fun rangeItemId(state: ItemReduceState?, param: String): Pair<ItemId?, ItemId?> {
        val fromItemId = state?.let { ItemId(it.token, it.tokenId) }
        val toItemId = if (param.isNotBlank()) {
            try {
                ItemId(Address.apply(param), EthUInt256.ZERO)
            } catch (e: Throwable) {
                Item.parseId(param)
            }
        } else null
        return Pair(fromItemId, toItemId)
    }

    private suspend fun verifyAllCompleted(topics: Iterable<Word>): Boolean {
        for (topic in topics) {
            val task = findTask(topic)
            if (task?.lastStatus != TaskStatus.COMPLETED) {
                return false
            }
        }
        return true
    }

    private suspend fun findTask(topic: Word): Task? {
        return taskRepository.findByTypeAndParam(ReindexTopicTaskHandler.TOPIC, topic.toString()).awaitFirstOrNull()
    }

    companion object {

        const val ITEM_REDUCE = "ITEM_REDUCE"
    }
}

data class ItemReduceState(
    val token: Address,
    val tokenId: EthUInt256
)
