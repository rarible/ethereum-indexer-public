package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.EnsDomainService
import com.rarible.protocol.nft.core.service.item.meta.ITEM_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemResolutionAbortedException
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.parseAttributes
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class EnsDomainsPropertiesResolver(
    private val ensDomainService: EnsDomainService,
    private val ensDomainsPropertiesProvider: EnsDomainsPropertiesProvider,
    nftIndexerProperties: NftIndexerProperties,
) : ItemPropertiesResolver {

    private val contractAddress: Address = Address.apply(nftIndexerProperties.ensDomainsContractAddress)

    override val name get() = "EnsDomains"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != contractAddress) {
            return null
        }
        return ensDomainsPropertiesProvider.get(itemId)?.also { ensDomainService.onGetProperties(itemId, it) }
    }
}

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class EnsDomainsPropertiesProvider(
    private val externalHttpClient: ExternalHttpClient,
    nftIndexerProperties: NftIndexerProperties,
) {

    private val contractAddress: Address = Address.apply(nftIndexerProperties.ensDomainsContractAddress)

    suspend fun get(itemId: ItemId): ItemProperties? {
        logMetaLoading(itemId.toString(), "get EnsDomains properties")

        // Let's try one more time in case of ENS API's 404 response
        for (i in 1..RETRIES_ON_404) {
            fetchProperties(itemId)?.let { return it }
        }

        // There is no reason to proceed with default resolvers (Rarible/OpenSea)
        throw ItemResolutionAbortedException()
    }

    private suspend fun fetchProperties(itemId: ItemId): ItemProperties? {
        val url = "${URL}/${NETWORK}/${contractAddress}/${itemId.tokenId.value}"
        val (req, timeout) = externalHttpClient.getResponseSpec(url = url, id = itemId.toString()) ?: return null
        return try {
            logMetaLoading(itemId.toString(), "parsing properties by URI: $url")

            val rawProperties = req?.bodyToMono<String>()
                ?.timeout(timeout)
                ?.onErrorResume(WebClientResponseException::class.java) {
                    return@onErrorResume when (it.rawStatusCode) {
                        HttpStatus.GONE.value() -> {
                            logMetaLoading(itemId.toString(), "Get GONE status, ens domain was expired!")
                            Mono.just(it.responseBodyAsString)
                        }
                        HttpStatus.NOT_FOUND.value() -> Mono.empty()
                        else -> Mono.error(it)
                    }
                }?.awaitFirstOrNull() ?: return null

            val json = JsonPropertiesParser.parse(itemId, rawProperties)
            json?.let { map(json, rawProperties) }
        } catch (e: Throwable) {
            logMetaLoading(itemId.toString(), "failed to get EnsDomains properties by $url due to ${e.message}}", true)
            throw ItemResolutionAbortedException()
        }
    }

    private fun map(json: ObjectNode, rawProperties: String): ItemProperties {
        return ItemProperties(
            name = json.path("name").asText(),
            description = json.path("description").asText(),
            image = json.path("image_url").asText(),
            imagePreview = null,
            imageBig = null,
            animationUrl = null,
            attributes = json.parseAttributes(milliTimestamps = true),
            rawJsonContent = rawProperties
        )
    }

    companion object {
        private const val URL = "https://metadata.ens.domains/"
        private const val NETWORK = "mainnet"
        private const val RETRIES_ON_404 = 2
    }
}
