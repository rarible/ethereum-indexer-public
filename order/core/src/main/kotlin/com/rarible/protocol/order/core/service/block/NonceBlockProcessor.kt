package com.rarible.protocol.order.core.service.block

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.service.ChangeCounterListener
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class NonceBlockProcessor(
    private val changeNonceListener: ChangeCounterListener,
    private val properties: OrderIndexerProperties
) : LogEventsListener {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val protocols = hashSetOf(
            properties.exchangeContractAddresses.seaportV1,
            properties.exchangeContractAddresses.looksrareV1
        )
        val nonceEvents = logs
            .filter { log -> log.address in protocols }
            .filter { log -> log.data is ChangeNonceHistory }

        val events = nonceEvents
            .map { log -> log.data as ChangeNonceHistory }
            .distinct()

        return LoggingUtils.withMarker { marker ->
            mono {
                events.forEach { event ->
                    changeNonceListener.onNewMakerNonce(event.source.toPlatform(), event.maker, event.newNonce.value.toLong())
                }
            }.toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Nonce logs process time: ${it.t1}ms") }
                .then()
        }
    }
}