package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.service.CryptoPunksMetaService
import com.rarible.protocol.nft.migration.configuration.IpfsProperties
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


@ChangeLog(order = "00014")
class ChangeLog00014UploadSvgsForCryptoPunks {

    @ChangeSet(id = "ChangeLog00014UploadSvgsForCryptoPunks.create", order = "1", author = "protocol")
    fun create(
        @NonLockGuarded cryptoPunksMetaService: CryptoPunksMetaService,
        @NonLockGuarded ipfsProperties: IpfsProperties
    ) = runBlocking<Unit> {
        val zipResponse = archive(ipfsProperties.cryptoPunksImagesUrl).awaitSingle()
        zipResponse.use { zipStream ->
            ZipInputStream(zipStream).use { unzipStream ->
                var entry: ZipEntry?
                val finished = AtomicInteger()
                var rateLimiter = 0
                val futures = mutableListOf<Deferred<*>>()
                while (unzipStream.nextEntry.also { entry = it } != null) {
                    entry?.takeIf { !it.isDirectory }?.let {
                        val content = unzipStream.readBytes()
                        futures.add(async(Dispatchers.IO) {
                            upload(
                                it.name,
                                content,
                                cryptoPunksMetaService,
                                ipfsProperties
                            )
                            logger.info("Uploaded ${finished.incrementAndGet()}/10000 images")
                        })
                        delay(betweenRequest)
                        if (++rateLimiter % ratelimit == 0) {
                            futures.awaitAll()
                            futures.clear()
                        }
                    }
                }
                futures.awaitAll()
            }
        }
        logger.info("Uploaded CryptoPunks svgs")
    }

    fun archive(url: String): Mono<InputStream> {
        return HttpClient.create()
            .baseUrl(url)
            .get()
            .responseContent()
            .aggregate()
            .asInputStream()
    }

    suspend fun upload(
        file: String, someByteArray: ByteArray,
        cryptoPunksMetaService: CryptoPunksMetaService,
        ipfsProperties: IpfsProperties
    ) {
        val response = postFile(file, someByteArray, ipfsProperties.uploadProxy)
        val id = file.filter { it.isDigit() }.toBigInteger()
        var punk = cryptoPunksMetaService.get(id).awaitSingle()
        punk = punk.copy(image = "${ipfsProperties.gateway}/${response.get("IpfsHash")}")
        cryptoPunksMetaService.save(punk)
        logger.info("$file was uploaded to ipfs with hash:${response.get("IpfsHash")}")
    }

    suspend fun postFile(filename: String?, someByteArray: ByteArray, url: String): Map<*, *> {
        val fileMap: MultiValueMap<String, String> = LinkedMultiValueMap()
        val contentDisposition: ContentDisposition = ContentDisposition
            .builder("form-data")
            .name("file")
            .filename(filename)
            .build()
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        fileMap.add("Content-Type", "image/svg+xml")
        val fileEntity = HttpEntity(someByteArray, fileMap)
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("file", fileEntity)

        val response = webClient.post()
            .uri(url)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(body))
            .retrieve()
            .bodyToMono(Map::class.java)
        return response.awaitSingle()
    }

    companion object {
        var webClient = WebClient.create()
        val ratelimit = 100
        val timeframe = 60_000L
        val betweenRequest = timeframe / ratelimit
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00014UploadSvgsForCryptoPunks::class.java)
    }
}
