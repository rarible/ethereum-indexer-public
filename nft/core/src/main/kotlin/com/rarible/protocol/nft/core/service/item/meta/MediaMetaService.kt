package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.content.meta.loader.ContentMetaLoader
import com.rarible.protocol.nft.core.service.IpfsService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MediaMetaService(
    private val ipfsService: IpfsService,
    @Value("\${api.proxy-url:}") private val proxyUrl: String,
    @Value("\${api.properties.media-meta-timeout}") private val timeout: Int,
    @Value("\${api.properties.media-meta-max-loaded-content-size:10000000}") private val maxLoadedContentSize: Long
) {

    private val contentMetaLoader = ContentMetaLoader(
        mediaFetchTimeout = timeout,
        mediaFetchMaxSize = maxLoadedContentSize,
        openSeaProxyUrl = proxyUrl
    )

    suspend fun getMediaMeta(url: String): ContentMeta? {
        val realUrl = ipfsService.resolveHttpUrl(url)
        return contentMetaLoader.fetchContentMeta(realUrl)
    }
}
