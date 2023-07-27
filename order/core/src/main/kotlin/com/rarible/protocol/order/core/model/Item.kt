package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address

data class Item(
    val contract: Address,
    val tokenId: EthUInt256
)
