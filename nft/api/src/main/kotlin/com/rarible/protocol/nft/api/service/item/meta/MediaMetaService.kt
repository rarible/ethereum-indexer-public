package com.rarible.protocol.nft.api.service.item.meta

import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.client.WebClientHelper
import com.rarible.core.common.blockingToMono
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.nft.core.model.MediaMeta
import com.sun.imageio.plugins.bmp.BMPMetadata
import com.sun.imageio.plugins.gif.GIFImageMetadata
import com.sun.imageio.plugins.jpeg.JPEGMetadata
import com.sun.imageio.plugins.png.PNGMetadata
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.Callable
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadata

@Component
class MediaMetaService(
    @Value("\${api.properties.media-meta-timeout}") private val timeout: Int
): CacheDescriptor<MediaMeta> {
    private val client = WebClient.builder()
        .clientConnector(WebClientHelper.createConnector(timeout, timeout, true))
        .build()

    override val collection: String = "cache_meta"

    override fun getMaxAge(value: MediaMeta?): Long =
        if (value == null) {
            DateUtils.MILLIS_PER_HOUR
        } else {
            Long.MAX_VALUE
        }

    override fun get(id: String): Mono<MediaMeta> = getMediaMeta(id)

    fun getMediaMeta(url: String): Mono<MediaMeta> {
        return LoggingUtils.withMarker { marker ->
            logger.info(marker, "getMediaMeta $url")
            when {
                url.endsWith(".mp4") ->
                    MediaMeta("video/mp4").toMono()
                url.endsWith(".webm") ->
                    MediaMeta("video/webm").toMono()
                url.endsWith(".mp3") ->
                    MediaMeta("audio/mp3").toMono()
                url.endsWith(".mpga") ->
                    MediaMeta("audio/mpeg").toMono()
                url.endsWith(".svg") ->
                    MediaMeta("image/svg+xml", 192, 192).toMono()
                else -> {
                    getMetadata(url)
                        .flatMap { (width, height, metadata) ->
                            when (metadata) {
                                is GIFImageMetadata -> MediaMeta("image/gif", metadata.imageWidth, metadata.imageHeight).toMono()
                                is JPEGMetadata -> MediaMeta("image/jpeg", width, height).toMono()
                                is BMPMetadata -> MediaMeta("image/bmp", width, height).toMono()
                                is PNGMetadata -> MediaMeta("image/png", width, height).toMono()
                                else -> Mono.error<MediaMeta>(IOException("Unknown metadata: " + metadata.javaClass.name))
                            }
                        }
                        .onErrorResume { ex ->
                            logger.warn(marker, "unable to get meta using image metadata", ex)
                            when {
                                url.endsWith(".gif") ->
                                    MediaMeta("image/gif").toMono()
                                url.endsWith(".jpg") ->
                                    MediaMeta("image/jpeg").toMono()
                                url.endsWith(".jpeg") ->
                                    MediaMeta("image/jpeg").toMono()
                                url.endsWith(".png") ->
                                    MediaMeta("image/png").toMono()
                                else -> getMimeType(url)
                                    .map { MediaMeta(it) }
                            }
                        }
                        .onErrorResume { ex ->
                            logger.warn(marker, "unable to get meta using HEAD request", ex)
                            Mono.empty()
                        }
                }
            }
        }
    }

    private fun getMimeType(url: String): Mono<String> {
        return client.head()
            .uri(URI(url))
            .exchange()
            .flatMap {
                val type = it.headers().contentType()
                if (type.isPresent) {
                    type.get().toString().toMono()
                } else {
                    Mono.empty()
                }
            }
    }

    private fun getMetadata(url: String): Mono<Triple<Int, Int, IIOMetadata>> {
        return Callable {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.readTimeout = timeout
            conn.connectTimeout = timeout
            conn.setRequestProperty("user-agent", "curl/7.73.0")
            conn.inputStream.use { get(it) }
        }.blockingToMono()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MediaMetaService::class.java)
    }
}

private fun get(ins: InputStream): Triple<Int, Int, IIOMetadata> {
    return ImageIO.createImageInputStream(ins).use { iis ->
        val readers = ImageIO.getImageReaders(iis)
        if (readers.hasNext()) {
            val r = readers.next()
            r.setInput(iis, true)
            try {
                Triple(r.getWidth(0), r.getHeight(0), r.getImageMetadata(0))
            } finally {
                r.dispose()
            }
        } else {
            throw IOException("reader not found")
        }
    }
}
