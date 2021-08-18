package com.rarible.protocol.gateway.filter.global

import com.rarible.protocol.gateway.filter.FilterOrder
import com.rarible.protocol.gateway.misc.setHeader
import kotlinx.coroutines.slf4j.MDCContext
import net.logstash.logback.marker.MapEntriesAppendingMarker
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*

@Component
class TracingGlobalFilter : GlobalFilter, Ordered {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getOrder() = FilterOrder.TRACING.order

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response

        val path = request.path.pathWithinApplication().value()
        val method = request.method

        val traceId =  request.headers.getFirst(X_RARIBLE_TRACE_ID)
            ?: run {
                UUID.randomUUID().toString().replace("-", "").also { generatedTraceId ->
                    request.setHeader(X_RARIBLE_TRACE_ID, generatedTraceId)
                }
            }

        val contextMap = mapOf(X_RARIBLE_TRACE_ID to traceId)
        val mdc = MDCContext(contextMap)
        val marker = MapEntriesAppendingMarker(contextMap)

        logger.info(marker, "$method: $path, ${request.queryParams}")

        return chain.filter(exchange)
            .doOnSuccess { logger.info(marker, "$method: $path, ${response.statusCode}") }
            .doOnError {  logger.info(marker, "$method: $path, error") }
            .doOnCancel { logger.info(marker, "$method: $path, canceled")  }
            .subscriberContext { it.put(MDCContext, mdc)}
    }

    private companion object {
       const val X_RARIBLE_TRACE_ID = "trace-id"
    }
}
