package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumEntityEvent
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import scalether.domain.Address

sealed class TokenEvent : EthereumEntityEvent<TokenEvent>() {
    abstract val eventTimeMarks: EthereumEventTimeMarks?

    data class TokenCreateEvent(
        val owner: Address,
        val name: String,
        val symbol: String,
        override val entityId: String,
        override val log: EthereumLog,
        override val eventTimeMarks: EthereumEventTimeMarks?,
    ) : TokenEvent()

    data class TokenChangeOwnershipEvent(
        val owner: Address,
        val previousOwner: Address,
        override val entityId: String,
        override val log: EthereumLog,
        override val eventTimeMarks: EthereumEventTimeMarks?,
    ) : TokenEvent()
}
