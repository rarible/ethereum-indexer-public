package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumEntityEvent
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import org.springframework.data.annotation.Transient
import scalether.domain.Address

sealed class TokenEvent : EthereumEntityEvent<TokenEvent>() {
    @Volatile
    @Transient
    var eventTimeMarks: EthereumEventTimeMarks? = null

    data class TokenCreateEvent(
        val owner: Address,
        val name: String,
        val symbol: String,
        override val entityId: String,
        override val log: EthereumLog,
    ) : TokenEvent()

    data class TokenChangeOwnershipEvent(
        val owner: Address,
        val previousOwner: Address,
        override val entityId: String,
        override val log: EthereumLog,
    ) : TokenEvent()
}
