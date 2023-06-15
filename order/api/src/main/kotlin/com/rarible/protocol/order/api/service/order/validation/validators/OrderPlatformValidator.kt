package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.order.api.exceptions.ValidationApiException
import com.rarible.protocol.order.api.service.order.validation.OrderVersionValidator
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import org.springframework.stereotype.Component

@Component
class OrderPlatformValidator(
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
) : OrderVersionValidator {

    override suspend fun validate(orderVersion: OrderVersion) {
        val isValidPlatform = when (orderVersion.platform) {
            Platform.RARIBLE -> true
            Platform.CMP -> featureFlags.enableCmpOrders
            Platform.OPEN_SEA,
            Platform.CRYPTO_PUNKS,
            Platform.LOOKSRARE,
            Platform.X2Y2,
            Platform.SUDOSWAP,
            Platform.BLUR -> false
        }
        if (isValidPlatform.not()) {
            throw ValidationApiException(
                "Order with platform ${orderVersion.platform} is not allowed",
            )
        }
    }
}
