package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.ifNotBlank
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.listener.service.opensea.SeaportOrderLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant

class SeaportOrdersLoadTaskHandler(
    private val seaportOrderLoader: SeaportOrderLoader,
) : TaskHandler<String> {

    override val type: String
        get() = SEAPORT_LOAD

    override suspend fun isAbleToRun(param: String): Boolean = true

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        val listedAfter = param.ifNotBlank()?.let { Instant.ofEpochSecond(param.toLong()) } ?: Instant.EPOCH
        var cursor = from
        do {
            val result = seaportOrderLoader.load(cursor)
            val orders = result.orders
            val lastFetchedList = orders.minOfOrNull { it.createdAt } ?: Instant.EPOCH
            cursor = result.next
            if (cursor != null) {
                emit(cursor)
            }
        } while (cursor != null && orders.isNotEmpty() && lastFetchedList > listedAfter)
    }

    companion object {
        const val SEAPORT_LOAD = "SEAPORT_LOAD"
    }
}
