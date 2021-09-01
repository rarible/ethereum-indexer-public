package com.rarible.protocol.nft.migration.mongock.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.item.ItemPropertyRepository
import com.rarible.protocol.nft.migration.configuration.IpfsProperties
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import scalether.domain.Address
import java.io.InputStreamReader


@ChangeLog(order = "00014")
class ChangeLog00014UploadSvgsForCryptoPunks {

    val rest = RestTemplate()

    @ChangeSet(id = "ChangeLog00014UploadSvgsForCryptoPunks.create", order = "1", author = "protocol")
    fun create(
        repository: ItemPropertyRepository,
        mapper: ObjectMapper,
        @NonLockGuarded nftIndexerProperties: NftIndexerProperties,
        @NonLockGuarded ipfsProperties: IpfsProperties
    ) = runBlocking<Unit> {
        val names = InputStreamReader(javaClass.getResourceAsStream(path)).readLines()
        logger.info("Found: ${names.size} images")
        names.forEach { upload(it, repository, mapper, nftIndexerProperties, ipfsProperties) }
        logger.info("Uploaded CryptoPunks svgs")
    }

    suspend fun upload(file: String,
               repository: ItemPropertyRepository,
               mapper: ObjectMapper,
               nftIndexerProperties: NftIndexerProperties,
               ipfsProperties: IpfsProperties) {
        val content = javaClass.getResourceAsStream("${path}/$file").readBytes()
        val response = postFile(file, content, ipfsProperties.uploadProxy)

        val number = file.filter { it.isDigit() }.toInt()
        val id = EthUInt256.of(number)
        val itemId = ItemId(Address.apply(nftIndexerProperties.cryptoPunksContractAddress), id)
        val str = repository.get(itemId).awaitSingle()
        val props: MutableMap<String, Any?> = mapper.readValue(str)
        props.put("name", "CryptoPunk #$number")
        props.put("image", "${ipfsProperties.gateway}/${response.get("IpfsHash")}")
        repository.save(itemId, mapper.writeValueAsString(props)).awaitSingle()
        logger.info("$file was uploaded to ipfs with hash:${response.get("IpfsHash")}")
    }

    fun postFile(filename: String?, someByteArray: ByteArray, url: String): Map<*, *> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        // to cheat the proxy
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:91.0) Gecko/20100101 Firefox/91.0")

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
        val requestEntity = HttpEntity(body, headers)
        return rest.exchange(url, HttpMethod.POST, requestEntity, Map::class.java).body
    }

    companion object {
        val path = "/data/cryptopunks/images"
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00014UploadSvgsForCryptoPunks::class.java)
    }
}
