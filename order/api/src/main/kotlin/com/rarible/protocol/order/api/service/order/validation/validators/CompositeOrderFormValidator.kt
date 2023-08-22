package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.core.common.asyncWithTraceId
import com.rarible.protocol.order.api.form.OrderForm
import com.rarible.protocol.order.api.service.order.validation.OrderFormValidator
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class CompositeOrderFormValidator(
    private val validators: List<OrderFormValidator>
) : OrderFormValidator {
    override suspend fun validate(form: OrderForm) {
        coroutineScope {
            validators.map {
                asyncWithTraceId {
                    try {
                        it.validate(form)
                    } catch (e: Exception) {
                        throw e
                    }
                }
            }.awaitAll()
        }
    }
}
