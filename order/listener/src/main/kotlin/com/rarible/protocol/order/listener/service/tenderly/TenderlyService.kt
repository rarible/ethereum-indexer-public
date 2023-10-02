package com.rarible.protocol.order.listener.service.tenderly

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.WebClientFactory
import com.rarible.protocol.order.core.model.BuyTx
import com.rarible.protocol.order.core.model.tenderly.TenderlyStat
import com.rarible.protocol.order.core.repository.tenderly.TenderlyRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Instant
import java.time.ZoneOffset

@Service
class TenderlyService(
    common: OrderIndexerProperties,
    properties: OrderListenerProperties,
    private val tenderlyRepository: TenderlyRepository
) {

    private val props = properties.tenderlyProperties
    private val blockchain = common.blockchain
    private val client = WebClientFactory.createClient(tenderlyUrl())
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun simulate(buyTx: BuyTx): SimulationResult {

        val (currentRequests, allowance) = optimisticLock {
            val date = Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
            val stat = tenderlyRepository.getById(date) ?: tenderlyRepository.save(TenderlyStat.create(date))
            val currentRequests = tenderlyRepository.requestsByMonth()
            val (updatedStat, allowance) = when {
                currentRequests < props.requestsPerMonth -> stat.incrementAttempt().incrementRequest() to true
                else -> stat.incrementAttempt() to false
            }
            tenderlyRepository.save(updatedStat)
            currentRequests to allowance
        }

        return if (allowance) {
            request(buyTx)
        } else {
            logger.warn("Limit of requests for tenderly was reached $currentRequests")
            SimulationResult(
                status = false,
                error = "Limit of requests was reached $currentRequests",
                reachLimit = true
            )
        }
    }

    suspend fun hasCapacity(): Boolean {
        return tenderlyRepository.requestsByMonth() < props.requestsPerMonth
    }

    suspend fun request(buyTx: BuyTx): SimulationResult {
        val request = SimulateRequest(
            params = listOf(
                SimulateRequest.Param(
                    from = buyTx.from,
                    to = buyTx.to,
                    gas = "0x0",
                    value = "0x${buyTx.value.toString(16)}",
                    // Tx data
                    data = buyTx.data
                ),

                // the number of a block
                "latest",

                // override balance with 1kk eth
                // in theory there are erc20, we need to override special contract then
                mapOf(buyTx.from to SimulateRequest.StateOverride(balance = "0xd3c21bcecceda1000000"))
            )
        )
        return try {
            val url = tenderlyUrl()
            val result = client.post()
                .uri(props.apiKey)
                .body(BodyInserters.fromValue(request))
                .retrieve().awaitBody<SimulateResponse>()
            logger.info("Send simulation for contract ${buyTx.to} to $url with apiKey=${props.apiKey.take(5)}*****")
            when {
                result.result != null -> SimulationResult(status = result.result.status)
                else -> SimulationResult(status = false, error = result.error?.message)
            }
        } catch (e: Exception) {
            logger.error("Exception during request to tenderly for contract ${buyTx.to}", e)
            throw RuntimeException("Can't get response from tenderly for contract ${buyTx.to}")
        }
    }

    private fun tenderlyUrl(): String {
        val network = props.mapping[blockchain.name.lowercase()] ?: blockchain.name.lowercase()
        return props.url.replace(":network", network)
    }

    data class SimulateRequest(
        val id: Int = 0,
        val jsonrpc: String = "2.0",
        val method: String = "tenderly_simulateTransaction",
        val params: List<Any>
    ) {

        data class Param(
            val from: String,
            val to: String,
            val gas: String = "0x0",

            // Gas is free, that's perfect for simulation
            val gasPrice: String = "0x0",
            val value: String,
            val data: String
        )

        data class StateOverride(

            // in wei
            val balance: String,

            // there we can override any balance on the specific contract
            val stateDiff: Map<String, String>? = null
        )
    }

    data class SimulateResponse(
        val id: String,
        val jsonrpc: String,
        val result: ResponseResult?,
        val error: ResponseError?,
    ) {
        data class ResponseResult(

            // for now we check only status: false -- reverted, true -- fully executed
            val status: Boolean
        )
        data class ResponseError(
            val code: String,
            val message: String,
        )
    }
}

data class SimulationResult(
    val status: Boolean,
    val error: String? = null,
    val reachLimit: Boolean = false,
)
