package com.rarible.protocol.order.core.service.block.approval

import com.rarible.blockchain.scanner.framework.listener.AbstractLogRecordEventListener
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.EntityEventListeners
import com.rarible.protocol.order.core.model.SubscriberGroups
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ApprovalEntityEventListener(
    @Qualifier("approval-event-subscriber")
    approvalEventSubscribers: List<LogRecordEventSubscriber>,
    properties: OrderIndexerProperties
) : AbstractLogRecordEventListener(
    subscribers = approvalEventSubscribers,
    id = EntityEventListeners.orderHistoryListenerId(properties.blockchain),
    groupId = SubscriberGroups.APPROVAL_HISTORY
)