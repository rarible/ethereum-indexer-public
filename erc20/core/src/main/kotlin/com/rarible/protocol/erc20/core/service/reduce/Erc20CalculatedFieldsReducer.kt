package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import org.springframework.stereotype.Component

@Component
class Erc20CalculatedFieldsReducer : Reducer<Erc20Event, Erc20Balance> {
    override suspend fun reduce(entity: Erc20Balance, event: Erc20Event): Erc20Balance {
        val lastEvent = entity.revertableEvents.lastOrNull { it.log.status == EthereumBlockStatus.CONFIRMED }
        val updatedAt =
            // We get date of the block here
            lastEvent?.date?.toInstant() ?: entity.lastUpdatedAt
        val blockNumber =
            lastEvent?.log?.blockNumber ?: entity.blockNumber
        return entity.copy(lastUpdatedAt = updatedAt, blockNumber = blockNumber)
    }
}
