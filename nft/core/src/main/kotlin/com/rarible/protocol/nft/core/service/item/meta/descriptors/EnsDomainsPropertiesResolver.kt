package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.EnsDomainService
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
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
        return ensDomainsPropertiesProvider.get(itemId)?.also {  ensDomainService.onGetProperties(itemId, it)}
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
        val properties = LoggingUtils.withMarker { marker ->
            logger.info(marker, "get EnsDomains properties ${itemId.tokenId.value}")

            externalHttpClient.get("${URL}/${NETWORK}/${contractAddress}/${itemId.tokenId.value}")
                .bodyToMono<String>()
                .map {
                    val node = mapper.readTree(it) as ObjectNode
                    ItemProperties(
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
                .onErrorResume {
                    logMetaLoading(
                        itemId,
                        "EnsDomains: failed to get properties" + if (it is WebClientResponseException) {
                            " ${it.rawStatusCode}: ${it.statusText}"
                        } else {
                            ""
                        },
                        warn = true
                    )
                    return@onErrorResume if (it is WebClientResponseException && it.rawStatusCode == 404) {
                        Mono.just(PROPERTIES_NOT_FOUND)
                    } else {
                        Mono.empty()
                    }
                }
        }.awaitFirstOrNull()

        return properties
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EnsDomainsPropertiesProvider::class.java)
        private const val URL = "https://metadata.ens.domains/"
        private const val NETWORK = "mainnet"

        val PROPERTIES_NOT_FOUND = ItemProperties(
            name = "Not found",
            description = null,
            image = null,
            imagePreview = null,
            imageBig = null,
            animationUrl = null,
            attributes = emptyList(),
            rawJsonContent = null,
        )
    }
}
