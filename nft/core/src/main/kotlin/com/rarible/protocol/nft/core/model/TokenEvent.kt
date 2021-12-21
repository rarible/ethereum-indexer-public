package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import scalether.domain.Address

sealed class TokenEvent : EthereumEntityEvent<TokenEvent>() {

    data class TokenCreateEvent(
        val owner: Address,
        val name: String,
        val symbol: String,
        val features: List<FeatureFlags>,
        val standard: TokenStandard,
        val status: ContractStatus,
        override val entityId: String,
        override val log: EthereumLog
    ) : TokenEvent()

    data class ItemChangeOwnershipEvent(
        val owner: Address,
        val previousOwner: Address,
        override val entityId: String,
        override val log: EthereumLog
    ) : TokenEvent()
}
