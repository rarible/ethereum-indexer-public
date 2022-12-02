package com.rarible.protocol.order.core.service.block.auction

import com.rarible.blockchain.scanner.ethereum.reduce.CompositeEntityEventListener
import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventsSubscriber
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.EntityEventListeners
import com.rarible.protocol.order.core.model.SubscriberGroups
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class AuctionEntityEventListener(
    @Qualifier("auction-event-subscriber")
    auctionEventSubscribers: List<EntityEventsSubscriber>,
    properties: OrderIndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo
) : CompositeEntityEventListener(
    entityEventsSubscribers = auctionEventSubscribers,
    id = EntityEventListeners.orderHistoryListenerId(environmentInfo.name, properties.blockchain),
    subscriberGroup = SubscriberGroups.AUCTION_HISTORY
)