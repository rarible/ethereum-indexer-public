package com.rarible.protocol.order.core.service

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.logging.addToMdc
import com.rarible.ethereum.domain.Blockchain
import com.rarible.opensea.client.Network
import com.rarible.opensea.client.SeaportProtocolClient
import com.rarible.opensea.client.model.OpenSeaError
import com.rarible.opensea.client.model.OperationResult
import com.rarible.opensea.client.model.v2.FulfillListingRequest
import com.rarible.opensea.client.model.v2.FulfillListingResponse
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.exception.EntityNotFoundApiException
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.metric.ExecutionError
import com.rarible.protocol.order.core.metric.OrderMetrics
import com.rarible.protocol.order.core.misc.Retry
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.OrderSeaportDataV1
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class SeaportSignatureResolver(
    private val orderCancelService: OrderCancelService,
    private val seaportClient: SeaportProtocolClient,
    private val orderRepository: OrderRepository,
    private val orderMetrics: OrderMetrics,
    private val properties: OrderIndexerProperties,
    private val environmentInfo: ApplicationEnvironmentInfo,
    private val featureFlags: OrderIndexerProperties.FeatureFlags
) {
    suspend fun resolveSeaportSignature(hash: Word): Binary {
        val order = orderRepository.findById(hash) ?: run {
            throw EntityNotFoundApiException("Order", hash)
        }
        val data = order.data as? OrderSeaportDataV1 ?: run {
            throw OrderDataException("Order $hash is not Seaport")
        }
        val request = FulfillListingRequest(
            hash = hash,
            fulfiller = order.maker,
            network = properties.blockchain.toNetwork(environmentInfo.name),
            protocolAddress = data.protocol
        )
        return Retry.retry(attempts = 5, delay = Duration.ofMillis(500)) {
            when (val result = seaportClient.getFulfillListingInfo(request)) {
                is OperationResult.Success -> getSignature(hash, result.result)
                is OperationResult.Fail -> handleError(hash, result)
            }
        }
    }

    private fun getSignature(hash: Word, result: FulfillListingResponse): Binary {
        return result
            .fulfillmentData
            .transaction
            .inputData
            .let {
                val signature = it.parameters?.signature ?: it.order?.signature
                if (signature != null) {
                    orderMetrics.onOrderExecution(Platform.OPEN_SEA)
                    signature
                } else {
                    orderMetrics.onOrderExecutionFailed(Platform.OPEN_SEA, ExecutionError.NO_SIGNATURE)
                    throw EntityNotFoundApiException("signature for hash", hash)
                }
            }
    }

    private suspend fun handleError(hash: Word, result: OperationResult.Fail<OpenSeaError>): Binary {
        logger.info("Get order $hash signature because of error: ${result.error.code}: ${result.error.message}")
        val (error, exception) = if (result.error.isGeneratingFulfillmentDataError()) {
            cancelOrder(hash, result)
            orderMetrics.onOrderExecutionFailed(Platform.OPEN_SEA, ExecutionError.SIGNATURE)
            ExecutionError.SIGNATURE to Retry.SkipRetryException(result.error.toApiException())
        } else {
            ExecutionError.API to result.error.toApiException()
        }
        orderMetrics.onOrderExecutionFailed(Platform.OPEN_SEA, error)
        throw exception
    }

    private fun Blockchain.toNetwork(env: String): Network {
        return when (this) {
            Blockchain.ETHEREUM -> {
                if (env == "prod") Network.ETHEREUM else Network.GOERLI
            }
            Blockchain.POLYGON -> {
                if (env == "prod") Network.POLYGON else Network.MUMBAI
            }
            Blockchain.OPTIMISM,
            Blockchain.MANTLE -> {
                throw IllegalArgumentException("Unsupported blockchain $this")
            }
        }
    }

    private suspend fun cancelOrder(hash: Word, result: OperationResult.Fail<OpenSeaError>) {
        if (featureFlags.cancelOrderOnGetSignatureError) {
            val eventTimeMarks = orderOffchainEventMarks()
            val order = orderCancelService.cancelOrder(hash, eventTimeMarks)
            logger.warn("Cancel order $hash because of error: ${result.error.code}: ${result.error.message}")
            if (order != null) {
                addToMdc("orderType" to order.type.name) {
                    logger.info("Unexpected order cancellation: ${order.type}:${order.hash}")
                }
            }
        }
    }

    private fun OpenSeaError.toApiException(): Throwable {
        return OrderDataException("Seaport error: $code: $message")
    }

    private val logger = LoggerFactory.getLogger(SeaportSignatureResolver::class.java)
}
