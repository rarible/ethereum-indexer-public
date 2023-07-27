package com.rarible.protocol.order.core.service.block.order

import com.rarible.blockchain.scanner.framework.listener.AbstractLogRecordEventListener
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.EntityEventListeners
import com.rarible.protocol.order.core.model.SubscriberGroups
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OrderEntityEventListener(
    @Qualifier("order-event-subscriber")
    orderEventSubscribers: List<LogRecordEventSubscriber>,
    properties: OrderIndexerProperties
) : AbstractLogRecordEventListener(
    subscribers = orderEventSubscribers,
    id = EntityEventListeners.orderHistoryListenerId(properties.blockchain),
    groupId = SubscriberGroups.ORDER_HISTORY
)
