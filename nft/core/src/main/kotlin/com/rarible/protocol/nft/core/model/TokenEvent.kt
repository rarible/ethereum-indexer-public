package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumEntityEvent
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.core.common.EventTimeMarks
import org.springframework.data.annotation.Transient
import scalether.domain.Address

sealed class TokenEvent : EthereumEntityEvent<TokenEvent>() {

    @Volatile
    @Transient
    var eventTimeMarks: EventTimeMarks? = null

    data class TokenCreateEvent(
        val owner: Address,
        val name: String,
        val symbol: String,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false
    ) : TokenEvent()

    data class TokenChangeOwnershipEvent(
        val owner: Address,
        val previousOwner: Address,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false
    ) : TokenEvent()

    data class TokenPauseEvent(
        val paused: Boolean,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false
    ) : TokenEvent()
}
