package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.CollectionEvent
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.model.indexerInNftBlockchainTimeMark

object TokenEventConverter {
    fun convert(source: ReversedEthereumLogRecord): TokenEvent? {
        val event = when (val data = source.data as? CollectionEvent) {
            is CreateCollection -> {
                TokenEvent.TokenCreateEvent(
                    owner = data.owner,
                    name = data.name,
                    symbol = data.symbol,
                    log = source.log,
                    entityId = data.id.prefixed(),
                )
            }
            is CollectionOwnershipTransferred -> {
                TokenEvent.TokenChangeOwnershipEvent(
                    owner = data.newOwner,
                    previousOwner = data.previousOwner,
                    log = source.log,
                    entityId = data.id.prefixed(),
                )
            }
            null -> null
        }
        event?.let { it.eventTimeMarks = indexerInNftBlockchainTimeMark(source.log) }
        return event
    }

    fun convert(source: LogEvent): TokenEvent? {
        return convert(LogEventToReversedEthereumLogRecordConverter.convert(source))
    }
}
