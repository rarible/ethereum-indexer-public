package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.order.api.exceptions.IncorrectOrderDataException
import com.rarible.protocol.order.core.model.AssetType.Companion.isLazy
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
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
}
