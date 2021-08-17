package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.data.RaribleOrderChecker
import com.rarible.protocol.nftorder.listener.service.BestOrderService
import org.slf4j.LoggerFactory

class BestOrderEvaluator(
    private val comparator: BestOrderComparator,
    private val provider: BestOrderProvider<*, *>
) {

    private val id = provider.entityId
    private val type = provider.entityType
    private val name = comparator.name

    companion object {
        private val logger = LoggerFactory.getLogger(BestOrderService::class.java)
    }

    suspend fun evaluateBestOrder(current: OrderDto?, updated: OrderDto): OrderDto? {
        return if (isAlive(updated)) {
            onAliveOrderUpdate(current, updated)
        } else {
            onDeadOrderUpdate(current, updated)
        }
    }

    private fun onAliveOrderUpdate(current: OrderDto?, updated: OrderDto): OrderDto {
        return if (current == null) {
            setBestOrder(updated)
        } else if (updated.hash == current.hash) {
            updateBestOrder(updated)
        } else {
            evaluateBestOrder(current, updated)
        }
    }

    private suspend fun onDeadOrderUpdate(current: OrderDto?, updated: OrderDto): OrderDto? {
        return if (current == null) {
            skipDeadOrder(updated)
        } else if (updated.hash == current.hash) {
            refetchDeadOrder(updated)
        } else {
            ignoreDeadOrder(current, updated)
        }
    }

    //--- Methods below presented as separate methods mostly for logging ---//

    // Set alive best Order for entity if there is no current best Order
    private fun setBestOrder(updated: OrderDto): OrderDto {
        logger.info(
            "Updated {} Order [{}] is alive, current Order for {} [{}] is null - using updated Order",
            name, updated, type, id
        )
        return updated
    }

    // Update current best Order with new data if current best Order is same as updated Order
    private fun updateBestOrder(updated: OrderDto): OrderDto {
        logger.info(
            "Updated {} Order [{}] is the same as current for {} [{}] - using updated Order",
            name, updated, type, id
        )
        return updated
    }

    // Select best Order between current and updated if they are different and alive
    private fun evaluateBestOrder(current: OrderDto, updated: OrderDto): OrderDto {
        val isCurrentPreferred = isPreferred(current)
        val isUpdatedPreferred = isPreferred(updated)

        val bestOrder = if (isCurrentPreferred != isUpdatedPreferred) {
            // if one of orders has preferred type and second hasn't return select preferred Order
            if (isCurrentPreferred) current else updated
        } else {
            // If both orders has preferred type or both are not preferred, comparing them
            comparator.compare(current, updated)
        }

        logger.info(
            "Evaluated {} for {} [{}] (current = [{}], updated = [{}]): hash = [{}]",
            name, type, id, current, updated, current.hash
        )
        return bestOrder
    }

    // Ignore dead Order when current best Order is not exist
    private fun skipDeadOrder(updated: OrderDto): OrderDto? {
        logger.info(
            "Updated {} Order [{}] is cancelled/filled, current Order for {} [{}] is null - nothing to update",
            name, updated, type, id
        )
        return null
    }

    // Re-fetch best order from indexer if updated Order is dead and the same as current best Order
    private suspend fun refetchDeadOrder(updated: OrderDto): OrderDto? {
        logger.info(
            "Updated {} Order [{}] is cancelled/filled, current Order for {} [{}] is the same - dropping it",
            name, updated, type, id
        )
        // It means, current best Order is not alive, we have to fetch actual best Order
        val fetched = provider.fetch()
        logger.info("Fetched {} for {} [{}] : [{}]", name, type, id, fetched)
        return fetched
    }

    // Ignore dead Order when current best Order exists
    private fun ignoreDeadOrder(current: OrderDto, updated: OrderDto): OrderDto {
        logger.info(
            "Updated {} Order [{}] is cancelled/filled, current Order for {} [{}] is [{}] - nothing to update",
            name, updated, type, id, current
        )
        return current
    }

    private fun isPreferred(order: OrderDto): Boolean {
        return RaribleOrderChecker.isRaribleOrder(order)
    }

    private fun isAlive(order: OrderDto): Boolean {
        return order.take.value != order.fill && !order.cancelled
    }

}