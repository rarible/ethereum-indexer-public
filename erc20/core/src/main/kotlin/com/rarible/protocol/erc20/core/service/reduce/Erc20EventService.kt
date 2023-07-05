package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.model.SubscriberGroup
import com.rarible.protocol.erc20.core.model.SubscriberGroups

class Erc20EventService(
    private val erc20EventListener: Erc20EventListener,
    properties: Erc20IndexerProperties
) : EntityEventListener {

    override val id: String = "protocol.${properties.blockchain.value}.erc20.token.history.listener"

    override val subscriberGroup: SubscriberGroup = SubscriberGroups.ERC20_HISTORY

    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        erc20EventListener.onEntityEvents(events)
    }
}