package com.rarible.protocol.erc20.core.listener

import com.rarible.protocol.erc20.core.converters.Erc20EventDtoConverter
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent
import com.rarible.protocol.erc20.core.producer.ProtocolEventPublisher

class KafkaErc20BalanceEventListener(
    private val protocolEventPublisher: ProtocolEventPublisher
) : Erc20BalanceEventListener {

    override suspend fun onUpdate(event: Erc20UpdateEvent) {
        val dto = Erc20EventDtoConverter.convert(event)
        protocolEventPublisher.publish(dto)
    }
}