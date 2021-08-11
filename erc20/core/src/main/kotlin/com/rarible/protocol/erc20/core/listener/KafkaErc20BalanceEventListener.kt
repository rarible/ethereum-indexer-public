package com.rarible.protocol.erc20.core.listener

import com.rarible.core.common.convert
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent
import com.rarible.protocol.erc20.core.producer.ProtocolEventPublisher
import org.springframework.core.convert.ConversionService

class KafkaErc20BalanceEventListener(
    private val conversionService: ConversionService,
    private val protocolEventPublisher: ProtocolEventPublisher
) : Erc20BalanceEventListener {

    override suspend fun onUpdate(event: Erc20UpdateEvent) {
        val dto = conversionService.convert<Erc20BalanceEventDto>(event)
        protocolEventPublisher.publish(dto)
    }
}