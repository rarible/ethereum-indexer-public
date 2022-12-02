package com.rarible.protocol.order.core.service.block.approval

import com.rarible.blockchain.scanner.ethereum.reduce.CompositeEntityEventListener
import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventsSubscriber
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.EntityEventListeners
import com.rarible.protocol.order.core.model.SubscriberGroups
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ApprovalEntityEventListener(
    @Qualifier("approval-event-subscriber")
    approvalEventSubscribers: List<EntityEventsSubscriber>,
    properties: OrderIndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo
) : CompositeEntityEventListener(
    entityEventsSubscribers = approvalEventSubscribers,
    id = EntityEventListeners.orderHistoryListenerId(environmentInfo.name, properties.blockchain),
    subscriberGroup = SubscriberGroups.APPROVAL_HISTORY
)