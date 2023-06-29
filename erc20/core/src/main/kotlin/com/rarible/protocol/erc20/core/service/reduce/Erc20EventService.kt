package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.model.SubscriberGroup
import com.rarible.protocol.erc20.core.model.SubscriberGroups
import org.springframework.stereotype.Component

@Component
class Erc20EventService(
    private val erc20EventReduceService: Erc20EventReduceService,
    private val erc20EventChainUpdateService: Erc20EventChainUpdateService,
    private val properties: Erc20IndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo,
) : EntityEventListener {

    private val env = environmentInfo.name

    override val id: String = "${env}.protocol.${properties.blockchain.value}.erc20.token.history.listener"

    override val subscriberGroup: SubscriberGroup = SubscriberGroups.ERC20_HISTORY

    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        if (properties.featureFlags.chainBalanceUpdateEnabled) {
            erc20EventChainUpdateService.onEntityEvents(events)
        } else {
            erc20EventReduceService.onEntityEvents(events)
        }
    }
}