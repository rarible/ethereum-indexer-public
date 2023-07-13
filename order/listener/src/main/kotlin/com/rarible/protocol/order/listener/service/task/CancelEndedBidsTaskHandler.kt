package com.rarible.protocol.order.listener.service.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CancelEndedBidsTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val orderUpdateService: OrderUpdateService,
) : TaskHandler<Long> {

    override val type: String
        get() = CANCEL_ENDED_BIDS

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val before = from?.let { Instant.ofEpochSecond(it) } ?: Instant.now()
        return orderRepository.findAllLiveBidsHashesLastUpdatedBefore(before)
            .map {
                val eventTimeMarks = orderTaskEventMarks()
                fixOrder(it)
                val order = orderRepository.findById(it) ?: error("Can't get order $it")
                if (order.isEndedBid()) {
                    orderUpdateService.update(it, eventTimeMarks)
                    logger.info("Cancel expired ended bid $it")
                }
                order.lastUpdateAt.epochSecond
            }
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
        private val logger: Logger = LoggerFactory.getLogger(CancelEndedBidsTaskHandler::class.java)
        const val CANCEL_ENDED_BIDS = "CANCEL_ENDED_BIDS"
    }
}
