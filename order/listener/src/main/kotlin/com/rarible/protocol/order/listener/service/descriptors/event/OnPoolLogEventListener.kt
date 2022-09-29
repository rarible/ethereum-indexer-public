package com.rarible.protocol.order.listener.service.descriptors.event

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.model.PoolHistoryType
import com.rarible.protocol.order.core.service.pool.listener.PoolActivityListener
import com.rarible.protocol.order.core.service.pool.listener.PoolOrderEventListener
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.EVENT)
class OnPoolLogEventListener(
    private val poolActivityListener: PoolActivityListener,
    private val poolOrderEventListener: PoolOrderEventListener,
) : OnLogEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val topics = PoolHistoryType.values().flatMap { it.topic }

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        onPoolEvent(logEvent, reverted =  false)
        poolActivityListener.onPoolEvent(logEvent)
    }.then()

    override fun onRevertedLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        onPoolEvent(logEvent, reverted =  true)
        poolActivityListener.onPoolEvent(logEvent)
        poolOrderEventListener.onPoolEvent(logEvent)
    }.then()

    private suspend fun onPoolEvent(logEvent: LogEvent, reverted: Boolean) {
        val dataType = logEvent.data::class.java.simpleName
        logger.info("Pool ${if (reverted) "reverted" else ""} log event: id=${logEvent.id}, dataType=$dataType")
    }
}