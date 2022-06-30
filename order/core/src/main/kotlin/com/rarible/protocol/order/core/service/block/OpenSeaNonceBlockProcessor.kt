package com.rarible.protocol.order.core.service.block

import com.rarible.core.common.toOptional
import com.rarible.core.logging.LoggingUtils
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.log.LogEventsListener
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.service.ChangeOpenSeaNonceListener
import com.rarible.protocol.order.core.service.ChangeSeaportCounterListener
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address

interface ChangeNonceListener {
    suspend fun onNewMakerNonce(maker: Address, newNonce: Long)
}

abstract class AbstractNonceBlockProcessor(
    private val changeNonceListener: ChangeNonceListener,
    private val protocolAddressProvider: () -> Address
) : LogEventsListener {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun postProcessLogs(logs: List<LogEvent>): Mono<Void> {
        val nonceEvents = logs
            .filter { log -> log.address == protocolAddressProvider() }
            .filter { log -> log.data is ChangeNonceHistory }

        val events = nonceEvents
            .map { log -> log.data as ChangeNonceHistory }
            .distinct()

        return LoggingUtils.withMarker { marker ->
            mono {
                events.forEach { event ->
                    changeNonceListener.onNewMakerNonce(event.maker, event.newNonce.value.toLong())
                }
            }.toOptional()
                .elapsed()
                .doOnNext { logger.info(marker, "Nonce logs process time: ${it.t1}ms") }
                .then()
        }
    }
}

@Service
class OpenSeaNonceBlockProcessor(
    changeNonceListener: ChangeOpenSeaNonceListener,
    properties: OrderIndexerProperties
) : AbstractNonceBlockProcessor(changeNonceListener, { properties.exchangeContractAddresses.openSeaV2 } )

@Service
class SeaportNonceBlockProcessor(
    changeNonceListener: ChangeSeaportCounterListener,
    properties: OrderIndexerProperties
) : AbstractNonceBlockProcessor(changeNonceListener, { properties.exchangeContractAddresses.seaportV1 } )