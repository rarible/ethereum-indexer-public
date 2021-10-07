package com.rarible.protocol.nft.core.service

import com.rarible.core.common.retryOptimisticLock
import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.core.service.token.TokenReduceService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class BlockProcessor(
    private val itemReduceService: ItemReduceService,
    private val tokenReduceService: TokenReduceService
) : LogEventsListener {

    override fun postProcessLogs(logs: MutableList<LogEvent>): Mono<Void> {
        return LoggingUtils.withMarker { marker ->
            Mono.`when`(
                itemReduceService.onItemHistories(logs.filter { it.data is ItemHistory }).retryOptimisticLock(),
                tokenReduceService.onTokenHistories(logs.filter { it.data is CreateCollection }).retryOptimisticLock()
            ).toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Process time: ${it.t1}ms") }
                .then()
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BlockProcessor::class.java)
    }
}
