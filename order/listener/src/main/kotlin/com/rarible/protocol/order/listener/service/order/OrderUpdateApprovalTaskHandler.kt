package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component

@Component
class OrderUpdateApprovalTaskHandler(
    orderRepository: OrderRepository,
    properties: OrderListenerProperties,
    private val orderUpdateService: OrderUpdateService,
    private val approveService: ApproveService,
    private val orderVersionRepository: OrderVersionRepository,
) : AbstractOrderUpdateStatusTaskHandler(orderRepository, properties) {

    override val type: String
        get() = UPDATE_ORDER_APPROVAL

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override suspend fun handleOrder(order: Order) {
        if (order.make.type.nft.not()) return
        val eventTimeMarks = orderOffchainEventMarks()
        logger.info("Checking approve: hash={}, platform={}, lastUpdated={}",
            order.hash, order.platform, order.lastUpdateAt
        )
        val onChainApprove = approveService.checkOnChainApprove(
            owner = order.maker,
            collection = order.make.type.token,
            platform =order.platform
        )
        if (order.approved != onChainApprove) {
            logger.warn("Approval did match with on-chain: hash={}, maker={}, collection={}",
                order.hash, order.maker, order.make.type.token
            )
            if (properties.fixApproval) {
                updateApprove(order, onChainApprove, eventTimeMarks)
            }
        }
    }

    private suspend fun updateApprove(order: Order, approve: Boolean, eventTimeMarks: EventTimeMarks) {
        val hash = order.hash
        val versions = mutableListOf<OrderVersion>()
        orderVersionRepository.findAllByHash(hash).toList(versions)
        val latestVersion = versions.maxByOrNull { it.id } ?: run {
            logger.error("Can't find latest orderVersion for $hash")
            return
        }
        orderVersionRepository.save(latestVersion.copy(approved = approve)).awaitFirst()
        orderUpdateService.updateApproval(order, approve, eventTimeMarks)
        val updated = orderRepository.findById(hash)
        logger.info("Order approved was updated: hash={}, status={}", updated?.hash, updated?.status)
    }

    companion object {
        const val UPDATE_ORDER_APPROVAL = "UPDATE_ORDER_APPROVAL"
    }
}