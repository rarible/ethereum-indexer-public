package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoPunksPropertiesResolver
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import scalether.domain.Address
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@ChangeLog(order = "00014")
class ChangeLog00014UploadSvgsForCryptoPunks {

    @ChangeSet(id = "ChangeLog00014UploadSvgsForCryptoPunks.create", order = "1", author = "protocol")
    fun uploadCryptoPunksSvgs(
        @NonLockGuarded cryptoPunksPropertiesResolver: CryptoPunksPropertiesResolver,
        @NonLockGuarded urlService: UrlService,
        @NonLockGuarded nftIndexerProperties: NftIndexerProperties
    ) = runBlocking<Unit> {
        if (nftIndexerProperties.blockchain != Blockchain.ETHEREUM) return@runBlocking
        val address = Address.apply(nftIndexerProperties.cryptoPunksContractAddress)
        if (address == Address.ZERO()) return@runBlocking
        val url = urlService.resolvePublicHttpUrl("QmVRJcGax4AavhGCJp4oxGC7264qPNdWHwQCsdSN8bs2YD")
            ?: return@runBlocking
        val zipResponse = downloadArchive(url).awaitSingle()
        zipResponse.use { zipStream ->
            ZipInputStream(zipStream).use { unzipStream ->
                var entry: ZipEntry?
                val finished = AtomicInteger()
                var rateLimiter = 0
                val futures = mutableListOf<Deferred<*>>()
                while (unzipStream.nextEntry.also { entry = it } != null) {
                    if (entry == null) break
                    if (entry!!.isDirectory) continue
                    val fileName = entry!!.name.substringAfterLast("/")
                    val content = unzipStream.readBytes()
                    futures.add(async(Dispatchers.IO) {
                        val imageUrl = save(
                            fileName,
                            content,
                            cryptoPunksPropertiesResolver
                        )
                        logger.info("Uploaded #${finished.incrementAndGet()}/10000 image: $imageUrl")
                    })
                    delay(betweenRequest)
                    if (++rateLimiter % rateLimit == 0) {
                        futures.awaitAll()
                        futures.clear()
                    }
                }
                futures.awaitAll()
            }
        }
        logger.info("Uploaded CryptoPunks svgs")
    }

    @Suppress("SameParameterValue")
    private fun downloadArchive(url: String): Mono<InputStream> {
        return HttpClient.create()
            .baseUrl(url)
            .get()
            .responseContent()
            .aggregate()
            .asInputStream()
    }

    suspend fun save(
        file: String,
        svgByteArray: ByteArray,
        cryptoPunksPropertiesResolver: CryptoPunksPropertiesResolver
    ): String {
        val id = file.filter { it.isDigit() }.toBigInteger()
        val imageUrl = String(svgByteArray)
        val punk = cryptoPunksPropertiesResolver.get(id).awaitSingle()
        cryptoPunksPropertiesResolver.save(punk.copy(image = imageUrl))
        return imageUrl
    }

    private companion object {
        const val rateLimit = 100
        const val timeframe = 60_000L
        const val betweenRequest = timeframe / rateLimit
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00014UploadSvgsForCryptoPunks::class.java)
    }
}
