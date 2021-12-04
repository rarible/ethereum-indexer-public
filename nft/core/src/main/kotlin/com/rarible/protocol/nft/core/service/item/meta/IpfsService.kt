package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.service.item.meta.descriptors.META_CAPTURE_SPAN_TYPE
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
@CaptureSpan(type = META_CAPTURE_SPAN_TYPE)
class IpfsService {
    private val webClient = WebClient.create()

    fun resolveHttpUrl(uri: String): String {
        val ipfsUri = if (uri.contains("/ipfs/")) {
            "ipfs:/${uri.substring(uri.lastIndexOf("/ipfs/"))}"
        } else {
            uri
        }
        return when {
            ipfsUri.startsWith("http") -> ipfsUri
            ipfsUri.startsWith("ipfs:///ipfs/") -> "$RARIBLE_IPFS/ipfs/${ipfsUri.removePrefix("ipfs:///ipfs/")}"
            ipfsUri.startsWith("ipfs://ipfs/") -> "$RARIBLE_IPFS/ipfs/${ipfsUri.removePrefix("ipfs://ipfs/")}"
            ipfsUri.startsWith("ipfs://") -> "$RARIBLE_IPFS/ipfs/${ipfsUri.removePrefix("ipfs://")}"
            ipfsUri.startsWith("Qm") -> "$RARIBLE_IPFS/ipfs/$ipfsUri"
            else -> "$RARIBLE_IPFS/${ipfsUri.trimStart('/')}"
        }.encodeHtmlUrl()
    }

    private fun String.encodeHtmlUrl(): String {
        return this.replace(" ", "%20")
    }

    suspend fun upload(
        fileName: String,
        someByteArray: ByteArray,
        contentType: String
    ): String {
        val response = postFile(fileName, someByteArray, contentType)
        val ipfsHash = response["IpfsHash"].toString()
        val url = resolveHttpUrl(ipfsHash)
        logger.info("$fileName was uploaded to ipfs with hash $ipfsHash: $url")
        return url
    }

    private suspend fun postFile(
        fileName: String,
        fileContent: ByteArray,
        contentType: String
    ): Map<*, *> {
        val fileMap: MultiValueMap<String, String> = LinkedMultiValueMap()
        val contentDisposition: ContentDisposition = ContentDisposition
            .builder("form-data")
            .name("file")
            .filename(fileName)
            .build()
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        fileMap.add("Content-Type", contentType)
        val fileEntity = HttpEntity(fileContent, fileMap)
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("file", fileEntity)

        val response = webClient.post()
            .uri(RARIBLE_UPLOAD_PROXY_URL)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(body))
            .retrieve()
            .bodyToMono(Map::class.java)
        return response.awaitSingle()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IpfsService::class.java)
        const val RARIBLE_IPFS = "https://rarible.mypinata.cloud"
        private const val RARIBLE_UPLOAD_PROXY_URL = "https://pinata.rarible.com/upload"
    }
}
