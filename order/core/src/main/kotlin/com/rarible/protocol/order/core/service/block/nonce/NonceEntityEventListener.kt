package com.rarible.protocol.order.core.service.block.nonce

import com.rarible.blockchain.scanner.framework.listener.AbstractLogRecordEventListener
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.EntityEventListeners
import com.rarible.protocol.order.core.model.SubscriberGroups
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class NonceEntityEventListener(
    @Qualifier("nonce-event-subscriber")
    nonceEventSubscribers: List<LogRecordEventSubscriber>,
    properties: OrderIndexerProperties
) : AbstractLogRecordEventListener(
    subscribers = nonceEventSubscribers,
    id = EntityEventListeners.orderHistoryListenerId(properties.blockchain),
    groupId = SubscriberGroups.NONCE_HISTORY
)