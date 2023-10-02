package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.WebClientFactory
import com.rarible.protocol.order.core.model.BuyTx
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.getOriginFees
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.awaitBody
import scalether.domain.Address
import java.math.BigInteger

@Service
class OrderTransactionService(
    common: OrderIndexerProperties,
    properties: OrderListenerProperties,
) {

    private val url = properties.txBackendProperties.url
    private val blockchain = common.blockchain
    private val client = WebClientFactory.createClient(url)
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun buyTx(order: Order, address: Address): BuyTx {
        val orderId = "$blockchain:${order.hash}"
        val request = BuyTxRequest(
            from = address.prefixed(),
            to = address.prefixed(),
            request = BuyTxRequest.Request(
                orderId = orderId,
                amount = order.make.value.value,
                originFees = order.data.getOriginFees()?.map { toPart(it) } ?: emptyList(),
                payouts = emptyList() // Doesn't use
            )
        )
        try {
            val result = client.post()
                .uri("v0.1/orders/buy-tx")
                .body(BodyInserters.fromValue(request))
                .retrieve().awaitBody<BuyTx>()
            logger.info("Received buy-tx for $orderId from $url")
            return result
        } catch (e: Exception) {
            logger.error("Exception during requesting but-tx for order $orderId", e)
            throw RuntimeException("Can't get buy-tx for order $orderId")
        }
    }

    fun toPart(part: Part): BuyTxRequest.Payout {
        return BuyTxRequest.Payout(part.account.toString(), part.value.value)
    }

    data class BuyTxRequest(
        val from: String,
        val to: String,
        val request: Request
    ) {
        data class Request(
            val orderId: String,
            val amount: BigInteger,
            val originFees: List<Payout>,
            val payouts: List<Payout>
        )
        data class Payout(
            val account: String,
            val value: BigInteger,
        )
    }
}
