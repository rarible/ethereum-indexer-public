package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.order.api.exceptions.IncorrectOrderDataException
import com.rarible.protocol.order.api.exceptions.OrderUpdateError
import com.rarible.protocol.order.api.exceptions.OrderUpdateErrorReason
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.model.AssetType.Companion.isLazy
import org.springframework.stereotype.Service

@Service
class OrderValidator(
    private val orderSignatureValidator: OrderSignatureValidator,
    private val lazyAssetValidator: LazyAssetValidator
) {
    suspend fun validateOrderVersion(orderVersion: OrderVersion) {
        val isValidOrderDataType = when (orderVersion.type) {
            OrderType.RARIBLE_V1 -> orderVersion.data is OrderDataLegacy
            OrderType.RARIBLE_V2 -> orderVersion.data is OrderRaribleV2DataV1
            OrderType.OPEN_SEA_V1 -> false
        }
        if (isValidOrderDataType.not()) {
            throw IncorrectOrderDataException("Order with type ${orderVersion.type} has invalid order data")
        }

        orderSignatureValidator.validate(orderVersion)

        orderVersion.make.type.takeIf { it.isLazy }
            ?.let { lazyAssetValidator.validate(it, "make") }

        orderVersion.take.type.takeIf { it.isLazy }
            ?.let { lazyAssetValidator.validate(it, "take") }
    }

    fun validate(existing: Order, update: OrderVersion) {
        if (existing.cancelled) {
            throw OrderUpdateError(OrderUpdateErrorReason.CANCELLED)
        }
        if (existing.data != update.data) {
            throw OrderUpdateError(OrderUpdateErrorReason.INVALID_UPDATE)
        }
        if (existing.start != update.start) {
            throw OrderUpdateError(OrderUpdateErrorReason.INVALID_UPDATE)
        }
        if (existing.end != update.end) {
            throw OrderUpdateError(OrderUpdateErrorReason.INVALID_UPDATE)
        }
        if (existing.taker != update.taker) {
            throw OrderUpdateError(OrderUpdateErrorReason.INVALID_UPDATE)
        }
        if (update.make.value < existing.make.value) {
            throw OrderUpdateError(OrderUpdateErrorReason.MAKE_VALUE_ERROR)
        }

        val newMaxTake = update.make.value * existing.take.value / existing.make.value
        if (newMaxTake < update.take.value) {
            throw OrderUpdateError(OrderUpdateErrorReason.TAKE_VALUE_ERROR)
        }
    }
}
