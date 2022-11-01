package com.rarible.protocol.order.listener.service.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class OrderUpdateApprovalTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val approveService: ApproveService,
    private val orderVersionRepository: OrderVersionRepository,
    private val properties: OrderListenerProperties,
) : TaskHandler<Long> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val type: String
        get() = UPDATE_ORDER_APPROVAL

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val taskParam = objectMapper.readValue(param, TaskParam::class.java)
        val lastUpdatedAt = from?.let { Instant.ofEpochSecond(it) }

        return orderRepository.findAllBeforeLastUpdateAt(
            lastUpdatedAt?.let { Date.from(it) },
            taskParam.status,
            taskParam.platform
        ).chunked(properties.parallelOrderUpdateStreams)
            .map { orders ->
                coroutineScope {
                    orders.map {
                        async { handleOrder(it) }
                    }.awaitAll()
                }
                orders.minOf { it.lastUpdateAt.toEpochMilli() }
            }
            .takeWhile { it < taskParam.listedAfter }
    }

    private suspend fun handleOrder(order: Order) {
        if (order.make.type.nft.not()) return
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
                updateApprove(order.hash, onChainApprove)
            }
        }
    }

    private suspend fun updateApprove(hash: Word, approve: Boolean) {
        val versions = mutableListOf<OrderVersion>()
        orderVersionRepository.findAllByHash(hash).toList(versions)
        val latestVersion = versions.maxByOrNull { it.id } ?: run {
            logger.error("Can't find latest orderVersion for $hash")
            return
        }
        orderVersionRepository.save(latestVersion.copy(approved = approve)).awaitFirst()
        orderUpdateService.update(hash)
        val updated = orderRepository.findById(hash)
        logger.info("Order approved was updated: hash={}, status={}", updated?.hash, updated?.status)
    }

    data class TaskParam(
        val status: OrderStatus,
        val platform: Platform,
        val listedAfter: Long
    )

    companion object {
        private val objectMapper = ObjectMapper().registerKotlinModule()
        const val UPDATE_ORDER_APPROVAL = "UPDATE_ORDER_APPROVAL"
    }
}