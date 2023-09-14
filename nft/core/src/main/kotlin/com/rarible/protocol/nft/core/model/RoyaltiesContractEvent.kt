package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import scalether.domain.Address

enum class RoyaltiesEventType {
    SET_CONTRACT_ROYALTIES,
}

sealed class RoyaltiesEvent(var type: RoyaltiesEventType) : EventData {
    abstract val token: Address
    abstract val parts: List<Part>

    override fun getKey(log: EthereumLog): String {
        return token.prefixed()
    }
}

data class SetRoyaltiesForContract(
    override val token: Address,
    override val parts: List<Part>
) : RoyaltiesEvent(RoyaltiesEventType.SET_CONTRACT_ROYALTIES)
