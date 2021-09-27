package com.rarible.protocol.nft.core.service.item.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.common.orNull
import com.rarible.core.common.toOptional
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.MediaMeta
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.whenComplete

@Service
class ContentMetaService(
    private val mapper: ObjectMapper,
    private val mediaMetaService: MediaMetaService,
    private val ipfsService: IpfsService,
    @Autowired(required = false) private val cacheService: CacheService?
) {
    fun getByProperties(properties: ItemProperties): Mono<ContentMeta> {
        val imageMediaMeta = when {
            properties.imagePreview != null -> getMediaMeta(ipfsService.resolveRealUrl(properties.imagePreview))
            properties.image != null -> getMediaMeta(ipfsService.resolveRealUrl(properties.image))
            else -> Mono.empty()
        }
        val animationMediaMeta = when {
            properties.animationUrl != null -> getMediaMeta(ipfsService.resolveRealUrl(properties.animationUrl))
            else -> Mono.empty()
        }
        return Mono.zip(
            imageMediaMeta.toOptional(),
            animationMediaMeta.toOptional()
        ) { p1, p2 ->
            ContentMeta(
                p1.orNull(),
                p2.orNull()
            )
        }
    }

    fun resetByProperties(properties: ItemProperties): Mono<Void> {
        return listOfNotNull(
            properties.image,
            properties.imagePreview,
            properties.animationUrl
        )
            .map { ipfsService.resolveRealUrl(it) }
            .map { cacheService?.reset(it, mediaMetaService) ?: Mono.empty() }
            .map { it.onErrorResume { Mono.empty() } }
            .whenComplete()
    }

    private fun getMediaMeta(url: String): Mono<MediaMeta> {
        return cacheService.get(url, mediaMetaService, true)
            .map { mapper.convertValue(it, MediaMeta::class.java) }
            .onErrorResume {
                Mono.empty()
            }
    }
}
