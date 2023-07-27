package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import java.math.BigInteger

object PayoutValidator {

    fun arePayoutsValid(orderData: OrderData): Boolean {
        val payouts = when (orderData) {
            is OrderRaribleV2DataV1 -> orderData.payouts
            is OrderRaribleV2DataV2 -> orderData.payouts
            else -> emptyList()
        }

        if (payouts.isEmpty()) {
            return true
        }

        val sum = payouts.sumOf { it.value.value }
        return sum == BigInteger.valueOf(10000)
    }
}
