package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.service.EventRevertService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent

open class ConfirmEventRevertService<T : BlockchainEntityEvent<T>>(
    nftIndexerProperties: NftIndexerProperties
) : EventRevertService<T> {
    private val confirmationBlocks = nftIndexerProperties.confirmationBlocks

    override fun canBeReverted(last: T, current: T): Boolean {
        require(last.status == BlockchainEntityEvent.Status.CONFIRMED)

        if (current.status != BlockchainEntityEvent.Status.CONFIRMED) {
            return true
        }

        val lastBlockNumber = checkNotNull(last.blockNumber)
        val currentBlockNumber = checkNotNull(current.blockNumber)
        return lastBlockNumber - currentBlockNumber <= confirmationBlocks
    }
}

