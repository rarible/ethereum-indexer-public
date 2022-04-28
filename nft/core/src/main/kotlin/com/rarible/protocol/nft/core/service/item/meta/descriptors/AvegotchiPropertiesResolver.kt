package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesWrapper
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesMapper
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class AvegotchiPropertiesResolver(
    private val openSeaPropertiesResolver: OpenSeaPropertiesResolver,
    sender: MonoTransactionSender,
    tokenRepository: TokenRepository,
    ipfsService: IpfsService,
    externalHttpClient: ExternalHttpClient,
    @Value("\${api.properties.request-timeout}") requestTimeout: Long
) : RariblePropertiesResolver(sender, tokenRepository, ipfsService, externalHttpClient, requestTimeout) {

    override val name get() = "Avegotchi"

    override suspend fun resolve(itemId: ItemId): ItemPropertiesWrapper {
        if (itemId.token != AVEGOTCHI_ADDRESS) {
            return wrapAsUnResolved(null)
        }

        val tokenUri = getUri(itemId)

        if (tokenUri.isNullOrBlank()) {
            logMetaLoading(itemId, "empty token URI", warn = true)
            return wrapAsUnResolved(null)
        }

        logMetaLoading(itemId, "got URI from token contract: $tokenUri")
        val properties = resolveByTokenUri(itemId, tokenUri)
        return if (properties != null) {
            wrapAsResolved(properties)
        } else {
            openSeaPropertiesResolver.resolve(itemId)
        }
    }

    override suspend fun getByUri(itemId: ItemId, uri: String): ItemProperties? {
        if (uri.isBlank()) {
            return null
        }

        val httpUrl = ipfsService.resolveInnerHttpUrl(uri)
        logMetaLoading(itemId, "getting properties by URI: $uri resolved as HTTP $httpUrl")
        val clientSpec = try {
            externalHttpClient.get(httpUrl)
        } catch (e: Exception) {
            logMetaLoading(itemId, "failed to parse URI: $httpUrl: ${e.message}", warn = true)
            return null
        }
        return clientSpec
            .onStatus (
                HttpStatus::is3xxRedirection
            ) {
                logMetaLoading(itemId, "failed to parse properties by URI: $httpUrl (3xx response)", warn = true)
                Mono.empty()
            }
            .bodyToMono<String>()
            .timeout(timeout)
            .onErrorResume {
                logMetaLoading(itemId, "failed to get properties by URI $httpUrl: ${it.message}", warn = true)
                Mono.empty()
            }
            .flatMap {
                logMetaLoading(itemId, "parsing properties by URI: $httpUrl")
                if (it.length > 1_000_000) {
                    logMetaLoading(itemId, "suspiciously big item properties ${it.length} for $httpUrl", warn = true)
                }
                mono {
                    val json = JsonPropertiesParser.parse(itemId, it)
                    json?.let { JsonPropertiesMapper.map(itemId, json) }
                }
            }
            .onErrorResume {
                logMetaLoading(itemId, "failed to parse properties by URI: $httpUrl", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }

    companion object {
        val AVEGOTCHI_ADDRESS: Address = Address.apply("0x1906fd9c4ac440561f7197da0a4bd2e88df5fa70")
    }
}

