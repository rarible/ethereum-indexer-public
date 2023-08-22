package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.api.configuration.OrderIndexerApiProperties
import com.rarible.protocol.order.api.form.OrderForm
import com.rarible.protocol.order.api.service.order.validation.OrderFormValidator
import com.rarible.protocol.order.core.exception.ValidationApiException
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class OrderStartEndDateValidator(
    private val apiProperties: OrderIndexerApiProperties,
) : OrderFormValidator {

    override suspend fun validate(form: OrderForm) {
        when {
            form.end == 0L -> {
                throw ValidationApiException("Missed end date")
            }
            form.start != null && form.end <= form.start -> {
                throw ValidationApiException("End date must be greater than start")
            }
            apiProperties.maxOrderEndDate != null &&
                (form.end - nowMillis().epochSecond) > apiProperties.maxOrderEndDate -> {
                throw ValidationApiException(
                    "Maximum end date is ${Duration.ofSeconds(apiProperties.maxOrderEndDate)} from now"
                )
            }
        }
    }
}
