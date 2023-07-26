@file:OptIn(ExperimentalTime::class)

package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.protocol.dto.MetaContentDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.meta.EthImageProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.service.EnsDomainService
import com.rarible.protocol.nft.core.service.item.meta.ITEM_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.ItemResolutionAbortedException
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.parseAttributes
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.domain.Address
import kotlin.time.ExperimentalTime

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
        val properties = ensDomainsPropertiesProvider.get(itemId)?.let {
            val image = EthMetaContent(
                url = "https://raribleuserdata.org/ens/mainnet/${itemId.token}/${itemId.tokenId}/image",
                representation = MetaContentDto.Representation.ORIGINAL,
                properties = EthImageProperties()
            )
            val newProperties = it.copy(content = it.content.copy(imageOriginal = image))

            ensDomainService.onGetProperties(itemId, newProperties)

            newProperties
        }

        return properties
    }
}

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class EnsDomainsPropertiesProvider(
    private val externalHttpClient: ExternalHttpClient,
    nftIndexerProperties: NftIndexerProperties,
) {

    private val contractAddress: Address = Address.apply(nftIndexerProperties.ensDomainsContractAddress)
    private val retries = 3
    private val retryDelay = 1000L

    suspend fun get(itemId: ItemId): ItemProperties? {
        logMetaLoading(itemId.toString(), "get EnsDomains properties")

        // Backoff policy doesn't work here, it is not bounded by retries count
        for (i in 0..retries) {
            try {
                return fetchProperties(itemId)
            } catch (e: EnsResponseException) {
                when (e.status) {
                    // Expired - there is no sense to retry, abort in order to do not rewrite existing meta
                    EnsStatus.EXPIRED -> {
                        logMetaLoading(itemId.toString(), "Get GONE status, ens domain was expired!")
                        throw ItemResolutionAbortedException()
                    }
                    // Not found - ok, lets retry, it happens for new items sometimes
                    EnsStatus.NOT_FOUND -> {
                        if (i < retries) {
                            logMetaLoading(itemId, "RETRY $i")
                            delay(retryDelay)
                        }
                    }
                }
            } catch (e: Throwable) {
                // Any other error - abort immediately
                logMetaLoading(
                    itemId.toString(),
                    "failed to get EnsDomains properties by ${getUrl(itemId)} due to ${e.message}}",
                    true
                )
                throw ItemResolutionAbortedException()
            }
        }

        // Retries exhausted, just abort resolution
        throw ItemResolutionAbortedException()
    }

    private suspend fun fetchProperties(itemId: ItemId): ItemProperties? {
        val url = getUrl(itemId)
        val (req, timeout) = externalHttpClient.getResponseSpec(url = url, id = itemId.toString()) ?: return null

        logMetaLoading(itemId.toString(), "parsing properties by URI: $url")

        val rawProperties = req?.bodyToMono<String>()
            ?.timeout(timeout)
            ?.onErrorResume(WebClientResponseException::class.java) {
                return@onErrorResume when (it.rawStatusCode) {
                    HttpStatus.GONE.value() -> Mono.error(EnsResponseException(EnsStatus.EXPIRED))
                    HttpStatus.GONE.value() -> Mono.error(EnsResponseException(EnsStatus.NOT_FOUND))
                    else -> Mono.error(it)
                }
            }?.awaitFirstOrNull() ?: return null

        val json = JsonPropertiesParser.parse(itemId, rawProperties)

        return if (json == null || json.isEmpty) {
            null
        } else {
            map(json, rawProperties)
        }
    }

    private fun getUrl(itemId: ItemId): String {
        return "$URL/$NETWORK/$contractAddress/${itemId.tokenId.value}"
    }

    private fun map(json: ObjectNode, rawProperties: String): ItemProperties {
        return ItemProperties(
            name = json.path("name").asText(),
            description = json.path("description").asText(),
            attributes = json.parseAttributes(milliTimestamps = true),
            rawJsonContent = rawProperties,
            content = ContentBuilder.getItemMetaContent(
                imageOriginal = json.path("image_url").asText()
            )
        )
    }

    companion object {

        private const val URL = "https://metadata.ens.domains/"
        private const val NETWORK = "mainnet"
    }

    class EnsResponseException(
        val status: EnsStatus
    ) : RuntimeException()

    enum class EnsStatus {
        NOT_FOUND,
        EXPIRED
    }
}
