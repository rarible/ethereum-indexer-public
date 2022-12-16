package com.rarible.protocol.erc20.listener.service.subscribers

import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.SubscriberGroups
import com.rarible.protocol.erc20.listener.service.descriptors.erc20.WithdrawalLogDescriptor
import org.springframework.stereotype.Component

@Component
class WithdrawalLogSubscriber(descriptor: WithdrawalLogDescriptor) :
    AbstractBalanceLogEventSubscriber<Erc20TokenHistory>(SubscriberGroups.ERC20_HISTORY, descriptor)