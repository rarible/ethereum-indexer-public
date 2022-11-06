package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withSpan
import com.rarible.core.entity.reducer.service.EventReduceService
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

) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val delegate = EventReduceService(erc20BalanceService, erc20BalanceIdService, erc20BalanceTemplateProvider, erc20BalanceReducer)

    suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        withSpan(
            name = "onErc20Events",
            labels = listOf(
                "balanceId" to (events.firstOrNull()?.record?.asEthereumLogRecord()
                    ?.let { erc20EventConverter.convert(it) }
                    ?: ""))
        ) {
            try {
                events.mapNotNull { erc20EventConverter.convert(it.record.asEthereumLogRecord()) }
                    .let { delegate.reduceAll(it) }
            } catch (ex: Exception) {
                logger.error("Error on entity events $events", ex)
                throw ex
            }
        }
    }
}