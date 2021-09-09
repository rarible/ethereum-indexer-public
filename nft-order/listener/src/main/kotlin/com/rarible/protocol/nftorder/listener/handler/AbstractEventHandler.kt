package com.rarible.protocol.nftorder.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.client.exception.ProtocolApiResponseException
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientResponseException

abstract class AbstractEventHandler<T> : ConsumerEventHandler<T> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: T) {
        try {
            handleSafely(event)
        } catch (ex: ProtocolApiResponseException) {
            logger.error(
                "Unable to handle event [{}], received error from Protocol-API client: {}",
                event, ex.responseObject
            )
        } catch (ex: WebClientResponseException) {
            logger.error(
                "Unable to handle event [{}], unhandled Protocol-API exception with status [{}]" +
                        " and error message: {}", event, ex.statusCode.value(), ex.message
            )
        } catch (ex: Exception) {
            logger.error("Unexpected exception during handling event [{}]", event, ex)
        }
    }

    abstract suspend fun handleSafely(event: T)
}