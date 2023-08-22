package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.order.api.form.OrderForm

interface OrderFormValidator {
    suspend fun validate(form: OrderForm)
}
