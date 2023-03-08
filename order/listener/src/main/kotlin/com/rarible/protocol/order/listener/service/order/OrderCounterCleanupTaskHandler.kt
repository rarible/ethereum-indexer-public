package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

@Component
@Deprecated("Delete after execution")
class OrderCounterCleanupTaskHandler(
    private val orderRepository: OrderRepository
) : TaskHandler<String> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val type = "ORDER_COUNTER_CLEANUP_TASK_HANDLER"

    private val batch = 1000
    private val updateBatch = 32

    override fun getAutorunParams(): List<RunTask> = listOf(RunTask(""))

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        var fromHash = from
        do {
            val nextHash = next(fromHash)
            nextHash?.let {
                emit(nextHash)
                fromHash = nextHash
            }
        } while (nextHash != null)
    }

    private suspend fun next(hash: String?): String? {
        // There is no good index to filter by datatype/platform
        val criteria = hash?.let {
            Criteria().and("_id").gt(hash)
        } ?: Criteria()

        val query = Query(criteria)
            .with(Sort.by(Sort.Direction.ASC, "_id"))
            .limit(batch)

        val orders = orderRepository.search(query)
        val filtered = orders.filter { updateRequired(it) }

        coroutineScope {
            filtered.chunked(updateBatch)
                .forEach { batch ->
                    batch.map {
                        async { updateCounter(it) }
                    }.awaitAll()
                }
        }

        logger.info("Order hex counter updated: ${filtered.size} of ${orders.size}")

        return orders.lastOrNull()?.hash?.prefixed()
    }

    private fun updateRequired(order: Order): Boolean {
        return when (order.data) {
            is OrderBasicSeaportDataV1 -> true
            is OrderLooksrareDataV1 -> true
            else -> false
        }
    }

    // Just save it to overwrite 'counter' field in DB
    private suspend fun updateCounter(order: Order) {
        orderRepository.save(order)
    }

}