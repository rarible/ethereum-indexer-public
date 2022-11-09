package com.rarible.protocol.erc20.listener.service.subscribers

import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.SubscriberGroups
import com.rarible.protocol.erc20.listener.service.descriptors.erc20.DepositLogDescriptor
import org.springframework.stereotype.Component

@Component
class DepositLogSubscriber(descriptor: DepositLogDescriptor) :
    AbstractBalanceLogEventSubscriber<Erc20TokenHistory>(SubscriberGroups.ERC20_HISTORY, descriptor)