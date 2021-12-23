package com.rarible.protocol.nft.core.converters.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.CollectionEvent
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.TokenEvent

object TokenEventConverter {
    fun convert(source: ReversedEthereumLogRecord): TokenEvent? {
        return when (val data = source.data as? CollectionEvent) {
            is CreateCollection -> {
                TokenEvent.TokenCreateEvent(
                    owner = data.owner,
                    name = data.name,
                    symbol = data.symbol,
                    log = source.log,
                    entityId = data.id.prefixed()
                )
            }
            is CollectionOwnershipTransferred -> {
                TokenEvent.TokenChangeOwnershipEvent(
                    owner = data.newOwner,
                    previousOwner = data.previousOwner,
                    log = source.log,
                    entityId = data.id.prefixed()
                )
            }
            null -> null
        }
    }

    fun convert(source: LogEvent): TokenEvent? {
        return convert(LogEventToReversedEthereumLogRecordConverter.convert(source))
    }
}
