package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.service.EventRevertService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent
import org.springframework.stereotype.Component

@Component
class EntityEventRevertService(
    private val nftIndexerProperties: NftIndexerProperties
) : EventRevertService<BlockchainEntityEvent<*>> {
    private val confirmationBlocks = nftIndexerProperties.confirmationBlocks

    override fun canBeReverted(last: BlockchainEntityEvent<*>, current: BlockchainEntityEvent<*>): Boolean {
        return last.blockNumber - current.blockNumber >= confirmationBlocks
    }
}
