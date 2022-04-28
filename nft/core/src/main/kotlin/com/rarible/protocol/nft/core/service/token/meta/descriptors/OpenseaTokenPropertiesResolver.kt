package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.descriptors.TOKEN_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.descriptors.getInt
import com.rarible.protocol.nft.core.service.item.meta.descriptors.getText
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService.Companion.logProperties
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.time.Duration

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class OpenseaTokenPropertiesResolver(
    private val mapper: ObjectMapper,
    private val externalHttpClient: ExternalHttpClient,
    @Value("\${api.opensea.request-timeout}") private val requestTimeout: Long,
): TokenPropertiesResolver {

    override suspend fun resolve(id: Address): TokenProperties? {
        if (externalHttpClient.openseaUrl.isBlank()) return null
        val httpUrl = "${externalHttpClient.openseaUrl}/asset_contract/${id.prefixed()}"
        logProperties(id, "OpenSea: getting properties from $httpUrl")
        return externalHttpClient.get(httpUrl)
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
                println("$it")
                logProperties(id, "failed to parse properties by URI: $httpUrl", true)
                Mono.empty()
            }
            .awaitFirstOrNull()
    }

    override val order get() = Int.MAX_VALUE

    private fun parseJsonProperties(jsonBody: String): TokenProperties? {
        val node = mapper.readTree(jsonBody) as ObjectNode
        return TokenProperties(
            name = getName(node) ?: "Untitled",
            description = node.getText("description"),
            image = node.getText("image_url"),
            externalLink = node.getText("external_link"),
            sellerFeeBasisPoints = node.getInt("opensea_seller_fee_basis_points"),
            feeRecipient = node.getText("payout_address")?.let { Address.apply(it) },
        )
    }

    private fun getName(node: ObjectNode): String? {
        val name = node.getText("name")
        return if (name == OPEN_SEA_COLLECTION_DEFAULT_NAME) {
            node.get("collection")?.getText("name") ?: name
        } else {
            name
        }
    }

    private companion object {
        const val OPEN_SEA_COLLECTION_DEFAULT_NAME = "Unidentified contract"
    }
}
