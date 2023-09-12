package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import scalether.domain.Address
import java.math.BigInteger

data class TokenUriReveal(
    val contract: Address,
    val tokenIdFrom: BigInteger,
    val tokenIdTo: BigInteger,
) : EventData {
    override fun getKey(log: EthereumLog): String = contract.prefixed()
}
