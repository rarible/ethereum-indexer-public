package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.api.exceptions.OrderUpdateException
import com.rarible.protocol.order.core.model.AssetType.Companion.isLazy
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderRaribleV2Data
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import org.springframework.stereotype.Service

@Service
class OrderValidator(
    private val orderSignatureValidator: OrderSignatureValidator,
    private val lazyAssetValidator: LazyAssetValidator
) {
    suspend fun validateOrderVersion(orderVersion: OrderVersion) {
        val isValidOrderDataType = when (orderVersion.type) {
            OrderType.RARIBLE_V1 -> orderVersion.data is OrderDataLegacy
            OrderType.RARIBLE_V2 -> orderVersion.data is OrderRaribleV2Data
            OrderType.OPEN_SEA_V1 -> false
            OrderType.CRYPTO_PUNKS -> orderVersion.data is OrderCryptoPunksData
        }
        if (isValidOrderDataType.not()) {
            throw OrderUpdateException(
                "Order with type ${orderVersion.type} has invalid order data",
                EthereumOrderUpdateApiErrorDto.Code.INCORRECT_ORDER_DATA
            )
        }

        orderSignatureValidator.validate(orderVersion)

        orderVersion.make.type.takeIf { it.isLazy }
            ?.let { lazyAssetValidator.validate(it, "make") }

        orderVersion.take.type.takeIf { it.isLazy }
            ?.let { lazyAssetValidator.validate(it, "take") }
    }

    fun validate(existing: Order, update: OrderVersion) {
        if (existing.cancelled) {
            throw OrderUpdateException("Order is cancelled", EthereumOrderUpdateApiErrorDto.Code.ORDER_CANCELED)
        }
        if (existing.data != update.data) {
            throw OrderUpdateException(
                "Order update failed ('data' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (existing.start != update.start) {
            throw OrderUpdateException(
                "Order update failed ('start' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (existing.end != update.end) {
            throw OrderUpdateException(
                "Order update failed ('end' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (existing.taker != update.taker) {
            throw OrderUpdateException(
                "Order update failed ('taker' changed)", EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
        if (update.make.value < existing.make.value) {
            throw OrderUpdateException(
                "Order update failed ('make.value' less then current)",
                EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }

        val newMaxTake = update.make.value * existing.take.value / existing.make.value
        if (newMaxTake < update.take.value) {
            throw OrderUpdateException(
                "Order update failed ('take.value' greater than maximum available: $newMaxTake)",
                EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE
            )
        }
    }
}
