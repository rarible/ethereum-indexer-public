package com.rarible.protocol.order.api.filter

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class LogRequestWebFilter : WebFilter, Ordered {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain
    ): Mono<Void> {
        val request = exchange.request
        val response = exchange.response

        val path = request.path.pathWithinApplication().value()
        val method = request.method

        val extractDataToLog = "queries: ${request.queryParams}"

        return chain.filter(exchange)
            .doOnCancel {
                logger.info("$method: $path, $extractDataToLog, status: canceled")
            }
            .doOnError { ex ->
                logger.error("$method: $path, $extractDataToLog, status: ${response.statusCode}", ex)
            }
    }
}
