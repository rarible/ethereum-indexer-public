package com.rarible.protocol.order.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.time.Instant

data class ChangeNonceHistory(
    val maker: Address,
    val newNonce: EthUInt256,
    val date: Instant,
    val source: HistorySource
) : EventData {
    override fun getKey(log: EthereumLog): String {
        return "$maker.$source"
    }
}
