package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.misc.X_API_KEY
import com.rarible.protocol.nft.core.misc.createConnector
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.TOKEN_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.descriptors.UserAgentGenerator
import com.rarible.protocol.nft.core.service.item.meta.descriptors.getInt
import com.rarible.protocol.nft.core.service.item.meta.descriptors.getText
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService.Companion.logProperties
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.time.Duration

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class OpenseaTokenPropertiesResolver(
    private val mapper: ObjectMapper,
    @Value("\${api.opensea.url:}") private val openseaUrl: String,
    @Value("\${api.opensea.api-key:}") private val openseaApiKey: String,
    @Value("\${api.opensea.read-timeout}") private val readTimeout: Int,
    @Value("\${api.opensea.connect-timeout}") private val connectTimeout: Int,
    @Value("\${api.opensea.request-timeout}") private val requestTimeout: Long,
    @Value("\${api.proxy-url:}") private val proxyUrl: String
): TokenPropertiesResolver {

    private val exchangeStrategies = ExchangeStrategies.builder()
        .codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(262144 * 5)
        }
        .build()

    protected val client = WebClient.builder()
        .clientConnector(createConnector(connectTimeout, readTimeout, proxyUrl, true))
        .exchangeStrategies(exchangeStrategies)
        .build()

    override suspend fun resolve(id: Address): TokenProperties? {
        if (openseaUrl.isBlank()) return null
        val httpUrl = "$openseaUrl/asset_contract/${id.prefixed()}"
        logProperties(id, "OpenSea: getting properties from $httpUrl")
        val get = client.get().uri(httpUrl)
        if (openseaApiKey.isNotBlank()) {
            get.header(X_API_KEY, openseaApiKey)
        }
        if (proxyUrl.isNotBlank()) {
            get.header(HttpHeaders.USER_AGENT, UserAgentGenerator.generateUserAgent())
        }
        return get.retrieve()
            .bodyToMono<String>()
            .onErrorResume {
                logProperties(id,
                    "OpenSea: failed to get properties for URI: $httpUrl" + if (it is WebClientResponseException) {
                        " response: ${it.rawStatusCode}: ${it.statusText}"
                    } else {
                        ""
                    }, true
                )
                Mono.empty()
            }
            .flatMap {
                logProperties(id, "parsing properties by URI: $httpUrl")
                mono { parseJsonProperties(it) }
            }
            .timeout(Duration.ofMillis(requestTimeout))
            .onErrorResume {
                logProperties(id, "failed to parse properties by URI: $httpUrl", true)
                Mono.empty()
            }
            .awaitFirstOrNull()
    }

    override val order get() = Int.MAX_VALUE

    private fun parseJsonProperties(jsonBody: String): TokenProperties? {
        val node = mapper.readTree(jsonBody) as ObjectNode
        return TokenProperties(
            name = node.getText("name") ?: "Untitled",
            description = node.getText("description"),
            image = node.getText("image_url"),
            externalLink = node.getText("external_link"),
            sellerFeeBasisPoints = node.getInt("opensea_seller_fee_basis_points"),
            feeRecipient = node.getText("payout_address").let { Address.apply(it) } ?: null,
        )
    }
}
