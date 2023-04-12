package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import org.springframework.stereotype.Component

@Component
class Erc20CalculatedFieldsReducer : Reducer<Erc20Event, Erc20Balance> {
    override suspend fun reduce(entity: Erc20Balance, event: Erc20Event): Erc20Balance {
        val updatedAt =
            // We try to get timestamp of the latest blockchain event
            entity.revertableEvents.lastOrNull { it.log.status == EthereumLogStatus.CONFIRMED }?.date?.toInstant() ?:
            entity.lastUpdatedAt
        val blockNumber = entity.revertableEvents.lastOrNull { it.log.status == EthereumLogStatus.CONFIRMED }?.log?.blockNumber
        return entity.copy(lastUpdatedAt = updatedAt).withBlockNumber(blockNumber)
    }
}
