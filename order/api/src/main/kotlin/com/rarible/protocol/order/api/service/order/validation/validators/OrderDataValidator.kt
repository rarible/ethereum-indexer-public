package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.api.form.OrderForm
import com.rarible.protocol.order.api.service.order.validation.OrderFormValidator
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.exception.OrderUpdateException
import com.rarible.protocol.order.core.model.OrderAmmData
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2Data
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class OrderDataValidator : OrderFormValidator {

    override suspend fun validate(form: OrderForm) {
        val isValidOrderDataType = when (form.type) {
            OrderType.RARIBLE_V1 -> form.data is OrderDataLegacy
            OrderType.RARIBLE_V2 -> form.data is OrderRaribleV2Data
            OrderType.OPEN_SEA_V1, OrderType.SEAPORT_V1 -> false
            OrderType.CRYPTO_PUNKS -> form.data is OrderCryptoPunksData
            OrderType.X2Y2 -> form.data is OrderX2Y2DataV1
            OrderType.LOOKSRARE -> form.data is OrderLooksrareDataV1
            OrderType.LOOKSRARE_V2 -> form.data is OrderLooksrareDataV2
            OrderType.AMM -> form.data is OrderAmmData
        }
        if (isValidOrderDataType.not()) {
            throw OrderUpdateException(
                "Order with type ${form.type} has invalid order data",
                EthereumOrderUpdateApiErrorDto.Code.INCORRECT_ORDER_DATA
            )
        }
        if (!arePayoutsValid(form.data)) {
            throw OrderDataException("Payouts sum not equal 100%")
        }
    }

    private fun arePayoutsValid(orderData: OrderData): Boolean {
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
