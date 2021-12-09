package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.service.EventRevertService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class EntityEventRevertService(
    nftIndexerProperties: NftIndexerProperties
) : EventRevertService<ItemEvent> {
    private val confirmationBlocks = nftIndexerProperties.confirmationBlocks

    override fun canBeReverted(last: ItemEvent, current: ItemEvent): Boolean {
        if (current.status == BlockchainEntityEvent.Status.PENDING) {
            return false
        }
        if (last.status == BlockchainEntityEvent.Status.PENDING) {
            return false
        }
        val lastBlockNumber = last.blockNumber ?: error("Can't be null")
        val currentBlockNumber = current.blockNumber ?: error("Can't be null")
        return lastBlockNumber - currentBlockNumber >= confirmationBlocks
    }
}
