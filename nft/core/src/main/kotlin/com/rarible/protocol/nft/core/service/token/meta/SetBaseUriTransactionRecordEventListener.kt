package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.blockchain.scanner.framework.listener.AbstractTransactionRecordEventListener
import com.rarible.blockchain.scanner.framework.listener.TransactionRecordEventSubscriber
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.SubscriberGroups
import org.springframework.stereotype.Component

@Component
class SetBaseUriTransactionRecordEventListener(
    properties: NftIndexerProperties,
    transactionRecordEventSubscribers: List<TransactionRecordEventSubscriber>,
) : AbstractTransactionRecordEventListener(
    id = "protocol.${properties.blockchain.value}.nft.set-base-uri.listener",
    groupId = SubscriberGroups.SET_BASE_URI,
    subscribers = transactionRecordEventSubscribers,
)
