package com.rarible.protocol.order.listener.service.task

import com.rarible.core.logging.withTraceId
import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class CancelSudoSwapEnsDomainTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderCancelService: OrderCancelService,
) : TaskHandler<Long> {

    override val type: String
        get() = CANCEL_SUDOSWAP_ENS_DOMAIN_ORDERS

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("", null))
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> = flow<Long> {
        logger.info("Starting CancelSudoSwapEnsDomainTaskHandler")
        val sudoSwapCriteria = (Order::make / Asset::type / AssetType::nft isEqualTo true)
            .and(Order::platform).isEqualTo(Platform.SUDOSWAP)
            .and(Order::cancelled).isEqualTo(false)
            .and(Order::make / Asset::type / AssetType::token)
            .isEqualTo(Address.apply("0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85"))

        orderRepository.searchAll(Query(sudoSwapCriteria))
            .collect {
                orderCancelService.cancelOrder(it.hash, orderTaskEventMarks())
            }

        logger.info("Finished CancelSudoSwapEnsDomainTaskHandler")
    }.withTraceId()

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CancelSudoSwapEnsDomainTaskHandler::class.java)
        const val CANCEL_SUDOSWAP_ENS_DOMAIN_ORDERS = "CANCEL_SUDOSWAP_ENS_DOMAIN_ORDERS"
    }
}
