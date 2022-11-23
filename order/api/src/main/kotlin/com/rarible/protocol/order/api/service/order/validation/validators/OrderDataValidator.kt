package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.api.exceptions.OrderDataException
import com.rarible.protocol.order.api.exceptions.OrderUpdateException
import com.rarible.protocol.order.api.service.order.validation.OrderVersionValidator
import com.rarible.protocol.order.core.model.OrderAmmData
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2Data
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.Part
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class OrderDataValidator : OrderVersionValidator {

    private val logger = LoggerFactory.getLogger(javaClass)
    override suspend fun validate(orderVersion: OrderVersion) {
        val isValidOrderDataType = when (orderVersion.type) {
            OrderType.RARIBLE_V1 -> orderVersion.data is OrderDataLegacy
            OrderType.RARIBLE_V2 -> orderVersion.data is OrderRaribleV2Data
            OrderType.OPEN_SEA_V1, OrderType.SEAPORT_V1 -> false
            OrderType.CRYPTO_PUNKS -> orderVersion.data is OrderCryptoPunksData
            OrderType.X2Y2 -> orderVersion.data is OrderX2Y2DataV1
            OrderType.LOOKSRARE -> orderVersion.data is OrderLooksrareDataV1
            OrderType.AMM -> orderVersion.data is OrderAmmData
        }
        if (isValidOrderDataType.not()) {
            throw OrderUpdateException(
                "Order with type ${orderVersion.type} has invalid order data",
                EthereumOrderUpdateApiErrorDto.Code.INCORRECT_ORDER_DATA
            )
        }
        validatePayouts(orderVersion.data)
    }

    private fun validatePayouts(orderData: OrderData) {
        val payouts = when (orderData) {
            is OrderRaribleV2DataV1 -> orderData.payouts
            is OrderRaribleV2DataV2 -> orderData.payouts
            else -> emptyList()
        }

        if (payouts.isNotEmpty()) {
            val sum = payouts.sumOf { it.value.value }
            if (sum != BigInteger.valueOf(10000))
                throw OrderDataException("Payouts sum not equal 100%")
        }
    }
}
