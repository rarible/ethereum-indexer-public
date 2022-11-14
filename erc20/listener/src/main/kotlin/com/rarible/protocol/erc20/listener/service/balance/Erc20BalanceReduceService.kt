package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.reduce.service.ReduceService
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.BalanceReduceSnapshot
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent
import com.rarible.protocol.erc20.core.model.ReduceVersion
import com.rarible.protocol.erc20.core.service.reduce.Erc20BalanceFullReduceService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

typealias Erc20BalanceReduceServiceV1 = ReduceService<Erc20ReduceEvent, BalanceReduceSnapshot, Long, Erc20Balance, BalanceId>

@Service
class Erc20BalanceReduceService(
    private val properties: Erc20IndexerProperties,
    private val erc20BalanceReduceServiceV1: Erc20BalanceReduceServiceV1,

    private val erc20BalanceReduceEventRepository: Erc20BalanceReduceEventRepository,
    private val erc20BalanceFullReduceService: Erc20BalanceFullReduceService
) {
    fun update(key: BalanceId?, minMark: Long): Flux<BalanceId> = when (properties.featureFlags.reduceVersion) {
        ReduceVersion.V1 -> erc20BalanceReduceServiceV1.update(key, minMark)
        ReduceVersion.V2 -> updateV2(key, minMark)
    }

    fun updateV2(key: BalanceId?, minMark: Long): Flux<BalanceId> {
        val events = erc20BalanceReduceEventRepository.getBusinessEvents(key, minMark).doOnNext {
            logger.info("Balance reduce Erc20Event=$it")
        }
        return erc20BalanceFullReduceService.reduce(events.asFlow())
            .map { it.id }
            .asFlux()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Erc20BalanceReduceService::class.java)
    }
}
