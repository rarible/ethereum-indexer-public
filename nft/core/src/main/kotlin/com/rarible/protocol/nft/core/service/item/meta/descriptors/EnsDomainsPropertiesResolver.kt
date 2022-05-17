package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.EnsDomainService
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemResolutionAbortedException
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
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
    private val mapper = ObjectMapper()

    suspend fun get(itemId: ItemId): ItemProperties? {
        logMetaLoading(itemId, "get EnsDomains properties")

        // Let's try one more time in case of ENS API's 404 response
        for (i in 1..RETRIES_ON_404) {
            fetchProperties(itemId)?.let { return it }
        }

        // There is no reason to proceed with default resolvers (Rarible/OpenSea)
        throw ItemResolutionAbortedException()
    }

    private suspend fun fetchProperties(itemId: ItemId): ItemProperties? {
        val url = "${URL}/${NETWORK}/${contractAddress}/${itemId.tokenId.value}"
        return externalHttpClient.get(url)
            .bodyToMono<String>()
            .map { parse(it) }
            .onErrorResume {
                if (it is WebClientResponseException && it.statusCode == HttpStatus.NOT_FOUND) {
                    // For some reason Ens sporadically returns 404 for existing items
                    logMetaLoading(itemId, "failed to get EnsDomains properties by $url due to 404 response code", true)
                    Mono.empty()
                } else {
                    logMetaLoading(itemId, "failed to get EnsDomains properties by $url due to ${it.message}}", true)
                    throw ItemResolutionAbortedException()
                }
            }.awaitFirstOrNull()
    }

    private fun parse(json: String): ItemProperties {
        val node = mapper.readTree(json) as ObjectNode
        return ItemProperties(
            name = node.path("name").asText(),
            description = node.path("description").asText(),
            image = node.path("image_url").asText(),
            imagePreview = null,
            imageBig = null,
            animationUrl = null,
            attributes = node.parseAttributes(milliTimestamps = true),
            rawJsonContent = node.toString()
        )
    }

    companion object {

        private const val URL = "https://metadata.ens.domains/"
        private const val NETWORK = "mainnet"
        private const val RETRIES_ON_404 = 2
    }
}
