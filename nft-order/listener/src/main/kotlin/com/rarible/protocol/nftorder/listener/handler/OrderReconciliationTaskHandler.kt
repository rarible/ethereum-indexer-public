package com.rarible.protocol.nftorder.listener.handler

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nftorder.listener.service.OrderReconciliationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

@Component
class OrderReconciliationTaskHandler(
    private val orderReconciliationService: OrderReconciliationService
) : TaskHandler<String> {

    override val type = "ORDER_RECONCILIATION_JOB"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("", null))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return flow {
            var next = from
            do {
                next = orderReconciliationService.reconcileOrders(next)
                if (next != null) emit(next!!)
            } while (next != null)
        }
    }
}