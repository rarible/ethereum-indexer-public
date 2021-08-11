package com.rarible.protocol.nftorder.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.time.Instant

data class ItemTransfer(
    val owner: Address,
    val token: Address,
    val tokenId: EthUInt256,
    val date: Instant,
    val from: Address,
    val value: EthUInt256 = EthUInt256.ONE
)