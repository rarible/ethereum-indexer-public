package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import scalether.domain.Address
import java.time.Instant

data class ChangeNonceHistory(
    val maker: Address,
    val newNonce: EthUInt256,
    val date: Instant,
    val source: HistorySource = HistorySource.OPEN_SEA
) : EventData