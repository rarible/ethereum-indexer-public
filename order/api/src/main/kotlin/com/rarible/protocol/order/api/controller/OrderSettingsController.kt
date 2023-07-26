package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.EthOrderFeesDto
import com.rarible.protocol.order.api.configuration.OrderSettingsProperties
import com.rarible.protocol.order.core.model.OrderType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderSettingsController(
    private val orderSettingsProperties: OrderSettingsProperties
) : OrderSettingsControllerApi {

    override suspend fun getFees(): ResponseEntity<EthOrderFeesDto> {
        val fees = OrderType.values().associateBy(
            { it.name },
            { orderSettingsProperties.fees.getOrDefault(it.name, 0) }
        )
        return ResponseEntity.ok(EthOrderFeesDto(fees))
    }
}
