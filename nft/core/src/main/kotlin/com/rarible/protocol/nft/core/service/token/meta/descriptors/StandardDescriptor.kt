package com.rarible.protocol.nft.core.service.token.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.client.DefaultProtocolWebClientCustomizer
import com.rarible.protocol.contracts.erc1155.v1.rarible.RaribleToken
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.item.meta.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.getInt
import com.rarible.protocol.nft.core.service.item.meta.getText
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.time.Duration

@Component
class StandardDescriptor(
    private val sender: MonoTransactionSender,
    private val ipfsService: IpfsService,
    private val tokenRepository: TokenRepository,
    private val mapper: ObjectMapper,
    @Value("\${api.properties.request-timeout}") requestTimeout: Long
): TokenPropertiesDescriptor {

    private val timeout = Duration.ofMillis(requestTimeout)

    private val client = WebClient.builder().apply {
        DefaultProtocolWebClientCustomizer().customize(it)
    }.build()

    override suspend fun resolve(id: Address): TokenProperties? {
        val uri = getCollectionUri(id)
        val httpUrl = ipfsService.resolveRealUrl(uri!!)
        return client.get()
            .uri(httpUrl)
            .retrieve()
            .bodyToMono<String>()
            .timeout(timeout)
            .onErrorResume {
                logger.error("failed to get properties by URI: $httpUrl for token: $id")
                Mono.empty()
            }
            .flatMap {
                logger.info("parsing properties by URI: $httpUrl for token: $id")
                mono { parseJsonProperties(it) }
            }
            .onErrorResume {
                logger.error("failed to parse properties by URI: $httpUrl for token: $id")
                Mono.empty()
            }.awaitFirstOrNull()
    }

    override fun order() = Int.MIN_VALUE


    private fun parseJsonProperties(jsonBody: String): TokenProperties? {
        val node = mapper.readTree(jsonBody) as ObjectNode
        return TokenProperties(
            name = node.getText("name") ?: "Untitled",
            description = node.getText("description"),
            image = node.getText("image"),
            external_link = node.getText("external_link"),
            seller_fee_basis_points = node.getInt("seller_fee_basis_points"),
            fee_recipient = node.getText("fee_recipient").let { Address.apply(it) } ?: null,
        )
    }

    private suspend fun getCollectionUri(id: Address): String? {
        val token = tokenRepository.findById(id).awaitFirstOrNull() ?: return null
        return when (token.standard) {
            TokenStandard.ERC1155 -> RaribleToken(id, sender).contractURI()
            TokenStandard.ERC721 -> MintableToken(id, sender).contractURI()
            else -> Mono.empty()
        }.onErrorResume {
            logger.error("failed to get contract uri for token: ${it.message}")
            Mono.empty()
        }.awaitFirstOrNull()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(StandardDescriptor::class.java)
    }
}
