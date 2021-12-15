package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.contracts.erc1155.v1.rarible.RaribleToken
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.descriptors.TOKEN_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.descriptors.getInt
import com.rarible.protocol.nft.core.service.item.meta.descriptors.getText
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService.Companion.logProperties
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.time.Duration

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class StandardTokenPropertiesResolver(
    private val sender: MonoTransactionSender,
    private val ipfsService: IpfsService,
    private val tokenRepository: TokenRepository,
    private val mapper: ObjectMapper,
    private val externalHttpClient: ExternalHttpClient,
    @Value("\${api.opensea.request-timeout}") private val requestTimeout: Long,
): TokenPropertiesResolver {

    override suspend fun resolve(id: Address): TokenProperties? {
        val uri = getCollectionUri(id)
        return uri?.let {
            val url = ipfsService.resolveHttpUrl(it)
            logProperties(id, "$it was resolved to: $url")
            request(id, url)
        }
    }

    override val order get() = Int.MIN_VALUE

    private suspend fun request(id: Address, httpUrl: String) = externalHttpClient.get(httpUrl)
        .bodyToMono<String>()
        .onErrorResume {
            logProperties(id, "failed to get properties by URI: $httpUrl", true)
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
        }.awaitFirstOrNull()

    private fun parseJsonProperties(jsonBody: String): TokenProperties {
        val node = mapper.readTree(jsonBody) as ObjectNode
        return TokenProperties(
            name = node.getText("name") ?: "Untitled",
            description = node.getText("description"),
            image = node.getText("image"),
            externalLink = node.getText("external_link"),
            sellerFeeBasisPoints = node.getInt("seller_fee_basis_points"),
            feeRecipient = node.getText("fee_recipient").let { Address.apply(it) } ?: null,
        )
    }

    @Suppress("ReactiveStreamsUnusedPublisher")
    private suspend fun getCollectionUri(id: Address): String? {
        val token = tokenRepository.findById(id).awaitFirstOrNull() ?: return null
        return when (token.standard) {
            TokenStandard.ERC1155 -> RaribleToken(id, sender).contractURI()
            TokenStandard.ERC721 -> MintableToken(id, sender).contractURI()
            else -> Mono.empty()
        }.onErrorResume {
            logProperties(id, "failed to get contract uri for token: ${it.message}", true)
            Mono.empty()
        }.awaitFirstOrNull()
    }
}
