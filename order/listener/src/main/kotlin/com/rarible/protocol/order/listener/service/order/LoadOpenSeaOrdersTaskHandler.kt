package com.rarible.protocol.order.listener.service.order

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.core.task.TaskHandler
import com.rarible.opensea.client.model.OpenSeaOrder
import com.rarible.protocol.order.core.model.RawOpenSeaOrder
import com.rarible.protocol.order.core.repository.order.OpenSeaOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LoadOpenSeaOrdersTaskHandler(
    private val openSeaOrderRepository: OpenSeaOrderRepository,
    private val orderRepository: OrderRepository,
    private val openSeaOrderConverter: OpenSeaOrderConverter,
    private val orderUpdateService: OrderUpdateService,
    private val properties: OrderListenerProperties
) : TaskHandler<String> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val type: String
        get() = LOAD_OPEN_SEA_ORDERS

    override suspend fun isAbleToRun(param: String): Boolean {
        return properties.loadOpenSeaOrders
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return openSeaOrderRepository.getAll(from)
            .map { order ->
                handleOrder(order)
                order.id
            }
    }

    private suspend fun handleOrder(order: RawOpenSeaOrder) {
        val orderVersion = mapper
            .readValue<OpenSeaOrder>(order.value)
            .let { openSeaOrderConverter.convert(it) }
            ?: return

        if (orderRepository.findById(orderVersion.hash) == null) {
            orderUpdateService.save(orderVersion)
            logger.info("[internal] Saved new OpenSea order ${orderVersion.hash},")
        }
    }

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    companion object {
        const val LOAD_OPEN_SEA_ORDERS = "LOAD_OPEN_SEA_ORDERS"
    }
}
