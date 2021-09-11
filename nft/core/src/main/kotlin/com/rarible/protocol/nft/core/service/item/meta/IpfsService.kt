package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.api.configuration.IpfsProperties
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

@Service
class IpfsService(
    @Value("\${api.ipfs-url}") private val ipfsUrl: String,
    private val ipfsProperties: IpfsProperties
) {
    fun resolveIpfsUrl(uri: String): String =
        if (uri.contains("/ipfs/")) {
            "ipfs:/${uri.substring(uri.lastIndexOf("/ipfs/"))}"
        } else {
            uri
        }

    fun resolveRealUrl(uri: String): String {
        val ipfsProtocolUri = resolveIpfsUrl(uri)
        return when {
            ipfsProtocolUri.startsWith("http") -> ipfsProtocolUri
            ipfsProtocolUri.startsWith("ipfs:///ipfs/") -> "$ipfsUrl/ipfs/${ipfsProtocolUri.substring("ipfs:///ipfs/".length)}"
            ipfsProtocolUri.startsWith("ipfs://ipfs/") -> "$ipfsUrl/ipfs/${ipfsProtocolUri.substring("ipfs://ipfs/".length)}"
            ipfsProtocolUri.startsWith("Qm") -> "$ipfsUrl/ipfs/$ipfsProtocolUri"
            else -> "$ipfsUrl$ipfsProtocolUri"
        }
    }

    fun url(hash: String): String = "ipfs://ipfs/${hash}"

    suspend fun upload(file: String, someByteArray: ByteArray): String {
        val response = postFile(file, someByteArray, ipfsProperties.uploadProxy)
        logger.info("$file was uploaded to ipfs with hash:${response.get("IpfsHash")}")
        return response.get("IpfsHash").toString()
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
        const val IPFS_NEW_URL = "https://ipfs.rarible.com"
        val logger: Logger = LoggerFactory.getLogger(IpfsService::class.java)
    }
}
