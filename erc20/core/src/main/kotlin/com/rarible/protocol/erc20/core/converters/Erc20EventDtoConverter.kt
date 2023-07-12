package com.rarible.protocol.erc20.core.converters

import com.rarible.protocol.dto.Erc20AllowanceEventDto
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.erc20.core.misc.addIndexerOut
import com.rarible.protocol.erc20.core.misc.erc20OffchainEventMarks
import com.rarible.protocol.erc20.core.misc.toDto
import com.rarible.protocol.erc20.core.model.Erc20AllowanceEvent
import com.rarible.protocol.erc20.core.model.Erc20BalanceEvent
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent

object Erc20EventDtoConverter {

    fun convert(source: Erc20BalanceEvent): Erc20BalanceEventDto {
        return when (source) {
            is Erc20UpdateEvent -> Erc20BalanceUpdateEventDto(
                eventId = source.id,
                balanceId = source.balance.id.stringValue, // TODO do we need it?
                balance = Erc20BalanceDtoConverter.convert(source.balance),
                createdAt = source.balance.createdAt, // TODO do we need it?
                lastUpdatedAt = source.balance.lastUpdatedAt, //TODO do we need it?
                eventTimeMarks = (source.eventTimeMarks ?: erc20OffchainEventMarks()).addIndexerOut().toDto()
            )
            is Erc20AllowanceEvent -> Erc20AllowanceEventDto(
                eventId = source.id,
                balanceId = source.allowance.id.stringValue,
                allowance = Erc20AllowanceDtoConverter.convert(source.allowance),
                createdAt = source.allowance.createdAt,
                lastUpdatedAt = source.allowance.lastUpdatedAt,
                eventTimeMarks = (source.eventTimeMarks ?: erc20OffchainEventMarks()).addIndexerOut().toDto()
            )
        }
    }
}
