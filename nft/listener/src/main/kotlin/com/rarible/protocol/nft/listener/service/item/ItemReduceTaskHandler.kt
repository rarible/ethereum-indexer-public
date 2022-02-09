package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.Task
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemType
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
    private val taskRepository: TaskRepository
) : TaskHandler<ItemReduceState> {

    override val type: String
        get() = ITEM_REDUCE

    override suspend fun isAbleToRun(param: String): Boolean =
        verifyAllCompleted(ItemType.TRANSFER.topic + ItemType.ROYALTY.topic)

    override fun runLongTask(from: ItemReduceState?, param: String): Flow<ItemReduceState> {
        val to = if (param.isNotBlank()) ItemId(Address.apply(param), EthUInt256.ZERO) else null

        return itemReduceService.update(from = from?.let { ItemId(it.token, it.tokenId) }, to = to)
            .map { (token, tokenId) -> ItemReduceState(token, tokenId) }
            .windowTimeout(Int.MAX_VALUE, Duration.ofSeconds(5))
            .flatMap {
                it.next()
            }
            .asFlow()
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