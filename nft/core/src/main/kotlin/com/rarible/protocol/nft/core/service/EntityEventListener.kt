package com.rarible.protocol.nft.core.service

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.nft.core.model.SubscriberGroup

interface EntityEventListener {
    val id: String

    val groupId: SubscriberGroup

    suspend fun onEntityEvents(events: List<LogRecordEvent<ReversedEthereumLogRecord>>)
}
