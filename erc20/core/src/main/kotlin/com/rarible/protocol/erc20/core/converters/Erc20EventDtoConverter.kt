package com.rarible.protocol.erc20.core.converters

import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.dto.blockchainEventMark
import com.rarible.protocol.dto.offchainEventMark
import com.rarible.protocol.erc20.core.model.Erc20BalanceEvent
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent

object Erc20EventDtoConverter {

    fun convert(source: Erc20BalanceEvent): Erc20BalanceEventDto {
        val markName = "indexer-out_erc20"
        val eventEpochSeconds = source.event?.log?.blockTimestamp
        val marks = eventEpochSeconds?.let { blockchainEventMark(markName, it) } ?: offchainEventMark(markName)

        return when (source) {
            is Erc20UpdateEvent -> Erc20BalanceUpdateEventDto(
                eventId = source.id,
                balanceId = source.balance.id.stringValue, // TODO do we need it?
                balance = Erc20BalanceDtoConverter.convert(source.balance),
                createdAt = source.balance.createdAt, // TODO do we need it?
                lastUpdatedAt = source.balance.lastUpdatedAt, //TODO do we need it?
                eventTimeMarks = marks
            )
        }
    }
}
