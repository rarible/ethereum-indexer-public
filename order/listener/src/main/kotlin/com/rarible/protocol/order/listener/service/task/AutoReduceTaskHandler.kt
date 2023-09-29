package com.rarible.protocol.order.listener.service.task

import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.order.core.model.ADMIN_AUTO_REDUCE_TASK_TYPE
import com.rarible.protocol.order.core.repository.AutoReduceRepository
import com.rarible.protocol.order.core.repository.TempTaskRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.auction.AuctionReduceService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AutoReduceTaskHandler(
    private val taskRepository: TempTaskRepository,
    private val orderReduceService: OrderReduceService,
    private val auctionReduceService: AuctionReduceService,
    private val autoReduceRepository: AutoReduceRepository,
) : TaskHandler<String> {

    override val type: String
        get() = ADMIN_AUTO_REDUCE_TASK_TYPE

    override suspend fun isAbleToRun(param: String): Boolean {
        return !isReindexInProgress()
    }

    override fun runLongTask(from: String?, param: String): Flow<String> = flow<String> {
        logger.info("Starting AutoReduceTaskHandler")
        autoReduceRepository.findOrders().collect {
            orderReduceService.update(orderHash = Word.apply(it.id)).asFlow().collect()
            autoReduceRepository.removeOrder(it)
        }
        autoReduceRepository.findAuctions().collect {
            val hash = Word.apply(it.id)
            auctionReduceService.update(hash, Long.MAX_VALUE).asFlow().collect()
            autoReduceRepository.removeAuction(it)
        }
        logger.info("Finished AutoReduceTaskHandler")
    }.withTraceId()

    private suspend fun isReindexInProgress(): Boolean {
        return taskRepository
            .findByType("BLOCK_SCANNER_REINDEX_TASK")
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .firstOrNull() != null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutoReduceTaskHandler::class.java)
    }
}
