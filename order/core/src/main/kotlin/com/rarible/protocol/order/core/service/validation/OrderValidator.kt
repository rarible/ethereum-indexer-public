package com.rarible.protocol.order.core.service.validation

import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.model.AssetType.Companion.isLazy
import com.rarible.protocol.order.core.service.OrderReduceService
import org.springframework.stereotype.Service

@Service
class OrderValidator(
    private val orderSignatureValidator: OrderSignatureValidator,
    private val lazyAssetValidator: LazyAssetValidator
) {
    @Throws(
        IncorrectOrderDataException::class,
        OrderSignatureValidator.IncorrectSignatureException::class,
        LazyAssetValidator.InvalidLazyAssetException::class
    )
    suspend fun validate(orderVersion: OrderVersion) {
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
            throw OrderReduceService.OrderUpdateError(OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.CANCELLED)
        }
        if (existing.data != update.data) {
            throw OrderReduceService.OrderUpdateError(OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.INVALID_UPDATE)
        }
        if (existing.start != update.start) {
            throw OrderReduceService.OrderUpdateError(OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.INVALID_UPDATE)
        }
        if (existing.end != update.end) {
            throw OrderReduceService.OrderUpdateError(OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.INVALID_UPDATE)
        }
        if (existing.taker != update.taker) {
            throw OrderReduceService.OrderUpdateError(OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.INVALID_UPDATE)
        }
        if (update.make.value < existing.make.value) {
            throw OrderReduceService.OrderUpdateError(OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.MAKE_VALUE_ERROR)
        }

        val newMaxTake = update.make.value * existing.take.value / existing.make.value
        if (newMaxTake < update.take.value) {
            throw OrderReduceService.OrderUpdateError(OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.TAKE_VALUE_ERROR)
        }
    }

    class IncorrectOrderDataException(message: String) : RuntimeException(message)
}
