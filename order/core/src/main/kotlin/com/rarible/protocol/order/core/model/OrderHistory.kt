package com.rarible.protocol.order.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.ethereum.model.EventData
import io.daonomic.rpc.domain.Word

interface OrderHistory : EventData {
    val hash: Word

    override fun getKey(log: EthereumLog): String {
        return hash.prefixed()
    }
}
