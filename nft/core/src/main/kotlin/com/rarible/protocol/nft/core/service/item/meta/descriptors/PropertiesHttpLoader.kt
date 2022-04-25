package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class PropertiesHttpLoader(
    private val externalHttpClient: ExternalHttpClient,
    @Value("\${api.properties.request-timeout}") requestTimeout: Long
) {

    private val timeout = Duration.ofMillis(requestTimeout)

    suspend fun getByUrl(itemId: ItemId, httpUrl: String): String? {
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
            .bodyToMono<String>()
            .timeout(timeout)
            .onErrorResume {
                logMetaLoading(itemId, "failed to get properties by URI $httpUrl: ${it.message}", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }
}
