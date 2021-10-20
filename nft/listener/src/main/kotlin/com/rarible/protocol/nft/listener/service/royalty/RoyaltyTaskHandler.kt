package com.rarible.protocol.nft.listener.service.royalty

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.RoyaltyService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class RoyaltyTaskHandler(
    private val itemRepository: ItemRepository,
    private val royaltyService: RoyaltyService
) : TaskHandler<String> {

    override val type: String
        get() = ROYALTY_REDUCE

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("", null))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return itemRepository.findFromByToken(from?.let { Address.apply(it) } ?: Address.ZERO())
            .map { item ->
                royaltyService.getRoyaltyDeprecated(item.token, item.tokenId)
                item.token
            }.map { it.prefixed() }
    }

    companion object {
        const val ROYALTY_REDUCE = "ROYALTY_REDUCE"
    }
}
