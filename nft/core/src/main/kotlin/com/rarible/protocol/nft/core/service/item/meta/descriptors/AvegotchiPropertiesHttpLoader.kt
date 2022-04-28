package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Component
@CaptureSpan(type = SpanType.EXT)
class AvegotchiPropertiesHttpLoader(
    private val externalHttpClient: ExternalHttpClient,
    @Value("\${api.properties.request-timeout}") requestTimeout: Long
) : PropertiesHttpLoader(externalHttpClient, requestTimeout) {

    private val timeout = Duration.ofMillis(requestTimeout)

    override suspend fun getByUrl(itemId: ItemId, httpUrl: String): String? {
        if (httpUrl.isBlank()) {
            return null
        }

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
            }.awaitFirstOrNull()
    }
}
