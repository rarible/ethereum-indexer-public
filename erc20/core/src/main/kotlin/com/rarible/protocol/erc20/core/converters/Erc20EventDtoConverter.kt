package com.rarible.protocol.erc20.core.converters

import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.erc20.core.model.Erc20BalanceEvent
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object Erc20EventDtoConverter : Converter<Erc20BalanceEvent, Erc20BalanceEventDto> {

    override fun convert(event: Erc20BalanceEvent): Erc20BalanceEventDto {
        return when (event) {
            is Erc20UpdateEvent -> Erc20BalanceUpdateEventDto(
                eventId = event.id,
                balanceId = event.balance.id.stringValue,
                balance = Erc20BalanceDtoConverter.convert(event.balance),
                createdAt = event.balance.createdAt,
                lastUpdatedAt = event.balance.lastUpdatedAt
            )
        }
    }
}
