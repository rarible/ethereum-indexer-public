package com.rarible.protocol.order.core.service.block

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.service.ChangeOpenSeaNonceListener
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class OpenSeaNonceBlockProcessor(
    private val changeOpenSeaNonceListener: ChangeOpenSeaNonceListener,
) : LogEventsListener {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val nonceEvents = logs
            .filter { log -> log.data is ChangeNonceHistory }

        val events = nonceEvents
            .map { log -> log.data as ChangeNonceHistory }
            .distinct()

        return LoggingUtils.withMarker { marker ->
            mono {
                events.forEach { event ->
                    changeOpenSeaNonceListener.onNewMakerNonce(event.maker, event.newNonce.value.toLong())
                }
            }.toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "OpenSea nonce logs process time: ${it.t1}ms") }
                .then()
        }
    }
}