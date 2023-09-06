package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import scalether.domain.Address

data class TokenUriReveal(
    val id: Address,
    val tokenUri: String,
) : EventData {
    override fun getKey(log: EthereumLog): String = id.prefixed()
}
