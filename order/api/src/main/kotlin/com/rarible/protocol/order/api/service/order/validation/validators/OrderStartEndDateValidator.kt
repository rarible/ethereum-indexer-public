package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.api.configuration.OrderIndexerApiProperties
import com.rarible.protocol.order.core.exception.ValidationApiException
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.validator.OrderValidator
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class OrderStartEndDateValidator(
    private val apiProperties: OrderIndexerApiProperties,
) : OrderValidator {

    override val type: String = "start_end"

    override suspend fun validate(order: Order) {
        when {
            order.end == null || order.end == 0L -> {
                throw ValidationApiException("Missed end date")
            }
            order.start != null && order.end!! <= order.start!! -> {
                throw ValidationApiException("End date must be greater than start")
            }
            apiProperties.maxOrderEndDate != null &&
                (order.end!! - nowMillis().epochSecond) > apiProperties.maxOrderEndDate -> {
                throw ValidationApiException(
                    "Maximum end date is ${Duration.ofSeconds(apiProperties.maxOrderEndDate)} from now"
                )
            }
        }
    }

    override fun supportsValidation(order: Order): Boolean = true
}
