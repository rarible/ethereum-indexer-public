package com.rarible.protocol.nft.core.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.service.item.meta.descriptors.IPFS_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.descriptors.SVG_START
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
@CaptureSpan(type = IPFS_CAPTURE_SPAN_TYPE)
class IpfsService {

    fun resolveHttpUrl(uri: String): String {
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
                ipfsUri.startsWith("ipfs:///ipfs/") -> "$RARIBLE_IPFS/ipfs/${ipfsUri.removePrefix("ipfs:///ipfs/")}"
                ipfsUri.startsWith("ipfs://ipfs/") -> "$RARIBLE_IPFS/ipfs/${ipfsUri.removePrefix("ipfs://ipfs/")}"
                ipfsUri.startsWith("ipfs://") -> "$RARIBLE_IPFS/ipfs/${ipfsUri.removePrefix("ipfs://")}"
                ipfsUri.startsWith("ipfs:/") -> "$RARIBLE_IPFS/ipfs/${ipfsUri.removePrefix("ipfs:/")}"
                ipfsUri.startsWith("Qm") -> "$RARIBLE_IPFS/ipfs/$ipfsUri"
                else -> "$RARIBLE_IPFS/${ipfsUri.trimStart('/')}"
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

        const val RARIBLE_IPFS = "https://ipfs.io"
        private val CID_PATTERN = Pattern.compile(
            "Qm[1-9A-HJ-NP-Za-km-z]{44,}|b[A-Za-z2-7]{58,}|B[A-Z2-7]{58,}|z[1-9A-HJ-NP-Za-km-z]{48,}|F[0-9A-F]{50,}|f[0-9a-f]{50,}"
        )
    }
}
