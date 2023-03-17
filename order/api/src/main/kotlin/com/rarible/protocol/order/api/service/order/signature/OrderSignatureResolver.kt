package com.rarible.protocol.order.api.service.order.signature

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.domain.Blockchain
import com.rarible.opensea.client.Network
import com.rarible.opensea.client.SeaportProtocolClient
import com.rarible.opensea.client.model.v2.FulfillListingRequest
import com.rarible.protocol.order.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.order.api.exceptions.OrderDataException
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderSeaportDataV1
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderSignatureResolver(
    private val seaportClient: SeaportProtocolClient,
    private val orderRepository: OrderRepository,
    private val properties: OrderIndexerProperties,
    private val environmentInfo: ApplicationEnvironmentInfo
) {
    suspend fun resolveSeaportSignature(hash: Word): Binary {
        val order = orderRepository.findById(hash) ?: run {
            throw EntityNotFoundApiException("Order", hash)
        }
        val data = order.data as OrderSeaportDataV1 ?: run {
            throw OrderDataException("Order $hash is not Seaport")
        }
        val request = FulfillListingRequest(
            hash = hash,
            fulfiller = order.maker,
            network = properties.blockchain.toNetwork(environmentInfo.name),
            protocolAddress = data.protocol
        )
        return seaportClient
            .getFulfillListingInfo(request)
            .ensureSuccess()
            .fulfillmentData
            .transaction
            .inputData
            .parameters
            .signature
    }

    private fun Blockchain.toNetwork(env: String): Network {
        return when (this) {
            Blockchain.ETHEREUM -> {
                if (env == "prod") Network.ETHEREUM else Network.GOERLI
            }
            Blockchain.POLYGON -> {
                if (env == "prod") Network.POLYGON else Network.MUMBAI
            }
            Blockchain.OPTIMISM -> {
                throw IllegalArgumentException("Unsupported blockchain $this")
            }
        }
    }

    private val logger = LoggerFactory.getLogger(OrderSignatureResolver::class.java)
}