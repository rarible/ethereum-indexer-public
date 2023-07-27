package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.ReindexOwnerService
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import scalether.domain.Address

/**
 * Background job that reduces all items of an owner (specified by `param`).
 */
@Component
class ReduceOwnerItemsTaskHandler(
    private val taskRepository: TempTaskRepository,
    private val ownershipRepository: OwnershipRepository,
    private val itemReduceService: ItemReduceService,
) : TaskHandler<String> {

    override val type: String = ReindexOwnerService.ADMIN_REDUCE_OWNER_ITEMS

    override suspend fun isAbleToRun(param: String): Boolean {
        val owner = Address.apply(param)
        return owner !in findOwnersBeingIndexedNow()
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val owner = Address.apply(param)
        val fromOwnershipId = from?.let { OwnershipId.parseId(it) }
        return ownershipRepository.findByOwner(owner = owner, fromIdExcluded = fromOwnershipId).map {
            itemReduceService
                .update(
                    token = it.token,
                    tokenId = it.tokenId,
                    from = ItemId(it.token, it.tokenId),
                    to = ItemId(it.token, it.tokenId),
                    updateNotChanged = false
                ).asFlow().collect()
            it.id.toString()
        }
    }

    private suspend fun findOwnersBeingIndexedNow(): List<Address> {
        return taskRepository
            .findByType(ReindexOwnerService.ADMIN_REDUCE_OWNER_ITEMS)
            .filter { it.lastStatus != TaskStatus.COMPLETED && it.running }
            .map { Address.apply(it.param) }
            .toList()
    }
}
