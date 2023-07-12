package com.rarible.protocol.erc20.core.event

import com.rarible.protocol.erc20.core.converters.Erc20EventDtoConverter
import com.rarible.protocol.erc20.core.model.Erc20BalanceEvent

class KafkaErc20BalanceEventListener(
    private val erc20EventPublisher: Erc20EventPublisher
) : Erc20BalanceEventListener {

    override suspend fun onUpdate(event: Erc20BalanceEvent) {
        val dto = Erc20EventDtoConverter.convert(event)
        erc20EventPublisher.publish(dto)
    }
}