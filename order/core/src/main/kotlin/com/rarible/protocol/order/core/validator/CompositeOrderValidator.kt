package com.rarible.protocol.order.core.validator

import com.rarible.core.common.asyncWithTraceId
import com.rarible.protocol.order.core.metric.OrderValidationMetrics
import com.rarible.protocol.order.core.model.Order
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CompositeOrderValidator(
    override val type: String,
    private val validators: List<OrderValidator>,
    private val orderValidationMetrics: OrderValidationMetrics,
) : OrderValidator {

    override fun supportsValidation(order: Order): Boolean = true

    override suspend fun validate(order: Order) {
        coroutineScope {
            validators.filter { it.supportsValidation(order) }.map {
                asyncWithTraceId {
                    try {
                        it.validate(order)
                        orderValidationMetrics.onOrderValidationSuccess(order.platform, it.type)
                    } catch (e: Exception) {
                        orderValidationMetrics.onOrderValidationFail(order.platform, it.type)
                        throw e
                    }
                }
            }.awaitAll()
        }
    }
}
