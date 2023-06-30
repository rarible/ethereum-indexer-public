package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.misc.addIndexerIn
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import org.bson.BsonMaximumSizeExceededException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Erc20EventReduceService(
    private val erc20EventConverter: Erc20EventConverter,
    erc20BalanceService: Erc20BalanceService,
    erc20BalanceIdService: Erc20BalanceIdService,
    erc20BalanceTemplateProvider: Erc20BalanceTemplateProvider,
    erc20BalanceReducer: Erc20BalanceReducer,
    private val properties: Erc20IndexerProperties,
) : Erc20EventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val delegate = object : EventReduceService<BalanceId, Erc20MarkedEvent, Erc20Balance>(
        erc20BalanceService,
        erc20BalanceIdService,
        erc20BalanceTemplateProvider,
        erc20BalanceReducer
    ) {
        override fun isChanged(current: Erc20Balance, result: Erc20Balance): Boolean {
            return current.balance != result.balance
        }
    }

    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        try {
            events
                .mapNotNull {
                    erc20EventConverter.convert(
                        it.record.asEthereumLogRecord(),
                        it.eventTimeMarks.addIndexerIn()
                    )
                }
                .let { delegate.reduceAll(it) }
        } catch (ex: Exception) {
            val locations = events.map { it.record.asEthereumLogRecord() }
                .map { "${it.transactionHash}:${it.logIndex}:${it.minorLogIndex}" }
            logger.error("Error on entity events: $locations", ex)
            if (properties.featureFlags.skipBsonMaximumSize && ex is BsonMaximumSizeExceededException) {
                logger.warn("Skipped BsonMaximumSizeExceededException", ex)
            } else {
                throw ex
            }
        }
    }
}
