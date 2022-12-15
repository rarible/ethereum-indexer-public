package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withSpan
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.model.EntityEventListeners
import com.rarible.protocol.erc20.core.model.SubscriberGroup
import com.rarible.protocol.erc20.core.model.SubscriberGroups
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Erc20EventReduceService(
    private val erc20EventConverter: Erc20EventConverter,
    erc20BalanceService: Erc20BalanceService,
    erc20BalanceIdService: Erc20BalanceIdService,
    erc20BalanceTemplateProvider: Erc20BalanceTemplateProvider,
    erc20BalanceReducer: Erc20BalanceReducer,
    properties: Erc20IndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo,
) : EntityEventListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val delegate = EventReduceService(
        erc20BalanceService,
        erc20BalanceIdService,
        erc20BalanceTemplateProvider,
        erc20BalanceReducer
    )

    override val id: String = EntityEventListeners.erc20HistoryListenerId(environmentInfo.name, properties.blockchain)

    override val subscriberGroup: SubscriberGroup = SubscriberGroups.ERC20_HISTORY

    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        withSpan(
            name = "onErc20Events",
            labels = listOf(
                "balanceId" to (events.firstOrNull()?.record?.asEthereumLogRecord()
                    ?.let { erc20EventConverter.convert(it) }
                    ?: ""))
        ) {
            try {
                events
                    .mapNotNull { erc20EventConverter.convert(it.record.asEthereumLogRecord()) }
                    .let { delegate.reduceAll(it) }
            } catch (ex: Exception) {
                logger.error("Error on entity events $events", ex)
                throw ex
            }
        }
    }
}