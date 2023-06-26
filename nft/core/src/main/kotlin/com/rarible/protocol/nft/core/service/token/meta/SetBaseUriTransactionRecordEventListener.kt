package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.blockchain.scanner.framework.listener.AbstractTransactionRecordEventListener
import com.rarible.blockchain.scanner.framework.listener.TransactionRecordEventSubscriber
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.core.model.SubscriberGroups
import org.springframework.stereotype.Component

@Component
class SetBaseUriTransactionRecordEventListener(
    environmentInfo: ApplicationEnvironmentInfo,
    transactionRecordEventSubscribers: List<TransactionRecordEventSubscriber>,
) : AbstractTransactionRecordEventListener(
    id = "${environmentInfo.name}.protocol.ethereum.nft.set-base-uri.listener",
    groupId = SubscriberGroups.SET_BASE_URI,
    subscribers = transactionRecordEventSubscribers,
)