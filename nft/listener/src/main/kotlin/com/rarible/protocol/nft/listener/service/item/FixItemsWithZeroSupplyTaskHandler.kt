package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class FixItemsWithZeroSupplyTaskHandler(
    private val itemReduceService: ItemReduceService,
    private val itemRepository: ItemRepository
) : TaskHandler<String> {

    override val type
        get() = FIX_SUPPLY

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val criteria = Criteria().andOperator(
                Item::supply isEqualTo EthUInt256.ZERO,
                Item::deleted isEqualTo false
        )
        val query = Query().addCriteria(criteria)

        return itemRepository.search(query).map {
            updateOrder(it)
        }
    }

    private suspend fun updateOrder(item: Item): String {

        itemReduceService.update(item.token, item.tokenId).awaitFirstOrNull()
        val updatedItem = itemRepository.findById(item.id).awaitFirstOrNull()

        if (updatedItem == null) {
            logger.error("Can't find updated item: $item!")
            return item.toString()
        }

        if (updatedItem.deleted) {
            logger.info("Item was updated. $updatedItem")
        } else {
            logger.warn("Item wasn't updated! $updatedItem")
        }

        return item.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FixItemsWithZeroSupplyTaskHandler::class.java)
        const val FIX_SUPPLY = "FIX_SUPPLY"
    }
}
