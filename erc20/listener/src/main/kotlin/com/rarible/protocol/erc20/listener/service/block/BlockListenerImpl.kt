package com.rarible.protocol.erc20.listener.service.block

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.listener.service.balance.Erc20BalanceReduceService
import com.rarible.protocol.erc20.listener.service.owners.IgnoredOwnersResolver
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address

@Service
class BlockListenerImpl(
    private val balanceReduceService: Erc20BalanceReduceService,
    private val ignoredOwnersResolver: IgnoredOwnersResolver
) : LogEventsListener {

    private val ignoredOwners: Set<Address> by lazy { ignoredOwnersResolver.resolve() }

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> =
        postProcessBlock(logs)

    fun postProcessBlock(logs: List<LogEvent>): Mono<Void> {
        val reduceHistory = logs
            .filter { log -> (log.data as Erc20TokenHistory).owner !in ignoredOwners }
            .map { log -> Erc20ReduceEvent(log, log.blockNumber ?: 0) }

        return LoggingUtils.withMarker { marker ->
            mono { balanceReduceService.onEvents(reduceHistory) }
            .toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Process time: ${it.t1}ms") }
                .then()
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BlockListenerImpl::class.java)
    }
}