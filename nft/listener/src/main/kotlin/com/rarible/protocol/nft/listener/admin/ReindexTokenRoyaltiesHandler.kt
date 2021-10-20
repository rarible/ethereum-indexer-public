package com.rarible.protocol.nft.listener.admin

import com.rarible.contracts.erc1155.TransferBatchEvent
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.core.task.Task
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.nft.core.model.ReindexTokenItemRoyaltiesTaskParam
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.RoyaltyService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
class ReindexTokenRoyaltiesHandler(
    private val taskRepository: TaskRepository,
    private val royaltyService: RoyaltyService,
    private val itemRepository: ItemRepository
) : TaskHandler<String> {

    override val type: String
        get() = ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES

    override suspend fun isAbleToRun(param: String): Boolean {
        return verifyAllCompleted(
            TransferEvent.id(),
            TransferBatchEvent.id()
        )
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val reindexParam = ReindexTokenItemRoyaltiesTaskParam.fromParamString(param)

        return itemRepository.findTokenItems(reindexParam.token, from?.let { EthUInt256.of(it) })
            .map { item ->
                royaltyService.getRoyaltyDeprecated(item.token, item.tokenId)
                item.tokenId
            }.map { it.toString() }
    }

    private suspend fun verifyAllCompleted(vararg topics: Word): Boolean {
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
}
