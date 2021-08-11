package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.listener.log.domain.EventData
import scalether.domain.Address

data class CreateCollection(
    val id: Address,
    val owner: Address,
    val name: String,
    val symbol: String
) : EventData