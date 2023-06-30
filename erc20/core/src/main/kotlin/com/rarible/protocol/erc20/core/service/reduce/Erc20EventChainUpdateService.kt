package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.erc20.core.misc.addIndexerIn
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Erc20EventChainUpdateService(
    private val erc20EventConverter: Erc20EventConverter,
    private val delegate: Erc20EventReduceService,
    private val erc20BalanceService: Erc20BalanceService
) : Erc20EventListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        events
            .groupBy { it.record.asEthereumLogRecord().getKey() }
            .onEach { handleBalanceEvent(it.key, it.value) }
    }

    private suspend fun handleBalanceEvent(entityId: String, events: List<LogRecordEvent>) {
        try {
            val balanceId = BalanceId.parseId(entityId)
            val lastEvent = erc20EventConverter.convert(
                events.last().record.asEthereumLogRecord(),
                events.last().eventTimeMarks.addIndexerIn()
            )
            val balance = erc20BalanceService.onChainUpdate(balanceId, lastEvent)
            if (balance == null) {
                logger.error("Can't update balance $balanceId via blockchain")
                delegate.onEntityEvents(events)
            }
        } catch (ex: Throwable) {
            val locations = events.map { it.record.asEthereumLogRecord() }.map { "${it.transactionHash}:${it.logIndex}:${it.minorLogIndex}" }
            logger.error("Error on entity events: $locations", ex)
            throw ex
        }
    }
}