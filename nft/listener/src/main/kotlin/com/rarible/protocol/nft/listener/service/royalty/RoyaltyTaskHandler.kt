package com.rarible.protocol.nft.listener.service.royalty

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.RoyaltyService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class RoyaltyTaskHandler(
    private val itemRepository: ItemRepository,
    private val royaltyService: RoyaltyService,
    private val taskRepository: TaskRepository
) : TaskHandler<Address> {

    override val type: String
        get() = ROYALTY_REDUCE

    override suspend fun isAbleToRun(param: String): Boolean {
        return !verifyCompleted()
    }

    override fun runLongTask(from: Address?, param: String): Flow<Address> {
        return itemRepository.findFromByToken(from ?: Address.ZERO())
            .map { item ->
                royaltyService.getRoyalty(item.token, item.tokenId)
                item.token
            }
    }

    private suspend fun verifyCompleted(): Boolean {
        val task = taskRepository.findByTypeAndParam(ROYALTY_REDUCE, "").awaitFirstOrNull()
        return task?.lastStatus == TaskStatus.COMPLETED
    }

    companion object {
        const val ROYALTY_REDUCE = "ROYALTY_REDUCE"
    }
}
