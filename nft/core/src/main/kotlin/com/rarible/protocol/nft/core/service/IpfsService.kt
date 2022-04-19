package com.rarible.protocol.nft.core.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.IPFS_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.descriptors.SVG_START
import org.springframework.stereotype.Component
import java.util.*
import java.util.regex.Pattern

@Component
@CaptureSpan(type = IPFS_CAPTURE_SPAN_TYPE)
class IpfsService(
    ipfsProperties: NftIndexerProperties.IpfsProperties
) {

    private val innerGateway = ipfsProperties.ipfsGateway.split(",").map { it.trimEnd('/') }
    val publicGateway = ipfsProperties.ipfsPublicGateway.trimEnd('/')

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInnerHttpUrl(uri: String): String {
        val randomInnerGateway = innerGateway[Random().nextInt(innerGateway.size)]
        return resolveHttpUrl(uri, randomInnerGateway)
    }

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(uri: String): String {
        return resolveHttpUrl(uri, publicGateway)
    }

    fun resolveHttpUrl(uri: String, gateway: String): String {
        val ipfsUri = if (uri.contains("/ipfs/")) {
            val end = uri.substring(uri.lastIndexOf("/ipfs/"))
            val first = end.split("/")[2]
            if (isCid(first)) {
                "ipfs:/${end}"
            } else {
                uri
            }
        } else {
            uri
        }
        return if (ipfsUri.startsWith(SVG_START)) {
            ipfsUri
        } else {
            when {
                ipfsUri.startsWith("http") -> ipfsUri
                ipfsUri.startsWith("ipfs:///ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs:///ipfs/")}"
                ipfsUri.startsWith("ipfs://ipfs/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://ipfs/")}"
                ipfsUri.startsWith("ipfs://") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs://")}"
                ipfsUri.startsWith("ipfs:/") -> "$gateway/ipfs/${ipfsUri.removePrefix("ipfs:/")}"
                ipfsUri.startsWith("Qm") -> "$gateway/ipfs/$ipfsUri"
                else -> "$gateway/${ipfsUri.trimStart('/')}"
            }.encodeHtmlUrl()
        }
    }

    private fun String.encodeHtmlUrl(): String {
        return this.replace(" ", "%20")
    }

    fun isCid(test: String): Boolean {
        return CID_PATTERN.matcher(test).matches()
    }

    companion object {

        private val CID_PATTERN = Pattern.compile(
            "Qm[1-9A-HJ-NP-Za-km-z]{44,}|b[A-Za-z2-7]{58,}|B[A-Z2-7]{58,}|z[1-9A-HJ-NP-Za-km-z]{48,}|F[0-9A-F]{50,}|f[0-9a-f]{50,}"
        )
    }
}
