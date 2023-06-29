package com.rarible.protocol.order.listener.service.task

import com.rarible.core.logging.withTraceId
import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderStatus.Companion.ALL_EXCEPT_CANCELLED
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class CancelOrdersWithoutExpiredTimeTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val orderUpdateService: OrderUpdateService,
) : TaskHandler<Long> {

    override val type: String
        get() = CANCEL_ORDERS_WITHOUT_EXPIRED_TIME

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("", null))
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> = flow<Long> {
        logger.info("Starting CancelBidsWithoutExpiredTimeTaskHandler")
        val bidsCriteria = (Order::take / Asset::type / AssetType::nft isEqualTo true)
            .and(Order::platform).isEqualTo(Platform.RARIBLE)
            .and(Order::status).inValues(ALL_EXCEPT_CANCELLED)
            .and(Order::end).isEqualTo(null)

        orderRepository.searchAll(Query(bidsCriteria))
            .collect {
                processOrder(it)
            }

        logger.info("Bids processed. Start orders processing")

        val ordersCriteria = (Order::make / Asset::type / AssetType::nft isEqualTo true)
            .and(Order::platform).isEqualTo(Platform.RARIBLE)
            .and(Order::status).isEqualTo(OrderStatus.INACTIVE)
            .and(Order::end).isEqualTo(null)

        orderRepository.searchAll(Query(ordersCriteria))
            .collect {
                processOrder(it)
            }
        logger.info("Finished CancelBidsWithoutExpiredTimeTaskHandler")
    }.withTraceId()

    private suspend fun processOrder(
        it: Order
    ): Long {
        val eventTimeMarks = orderOffchainEventMarks()
        fixOrder(it.hash)
        orderUpdateService.update(it.hash, eventTimeMarks)
        return it.createdAt.epochSecond
    }

    private suspend fun fixOrder(hash: Word) {
        val orderVersions = orderVersionRepository.findAllByHash(hash).toList()
        if (orderVersions.isEmpty()) {
            val order = orderRepository.findById(hash) ?: return
            val orderVersion = OrderVersion(
                maker = order.maker,
                make = order.make,
                take = order.take,
                taker = order.taker,
                type = order.type,
                salt = order.salt,
                start = order.start,
                end = order.end,
                data = order.data,
                signature = order.signature,
                platform = order.platform,
                hash = order.hash,
                approved = order.approved,
                createdAt = order.createdAt,
                makePriceUsd = null,
                takePriceUsd = null,
                makePrice = null,
                takePrice = null,
                makeUsd = null,
                takeUsd = null
            )
            orderVersionRepository.save(orderVersion).awaitFirst()
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CancelOrdersWithoutExpiredTimeTaskHandler::class.java)
        const val CANCEL_ORDERS_WITHOUT_EXPIRED_TIME = "CANCEL_ORDERS_WITHOUT_EXPIRED_TIME"
    }
}
