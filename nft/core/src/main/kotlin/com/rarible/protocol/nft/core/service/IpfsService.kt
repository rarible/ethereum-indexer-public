package com.rarible.protocol.nft.core.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.IPFS_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.descriptors.SVG_START
import org.springframework.stereotype.Component
import java.net.URL
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
        // For internal calls original IPFS host should be replaced in order to avoid rate limit of the original gateway
        return resolveHttpUrl(uri, randomInnerGateway, true)
    }

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(uri: String): String {
        // For public IPFS urls we want to keep original gateway URL (if possible)
        return resolveHttpUrl(uri, publicGateway, false)
    }

    private fun resolveHttpUrl(uri: String, gateway: String, replaceOriginalHost: Boolean): String {
        // Embedded image, return 'as is'
        if (uri.startsWith(SVG_START)) {
            return uri
        }

        // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala
        checkForeignIpfsUri(uri, gateway, replaceOriginalHost)?.let { return it.encodeHtmlUrl() }

        // Checking prefixed IPFS URI like ipfs://Qmlalala
        checkIpfsAbstractUrl(uri, gateway)?.let { return it.encodeHtmlUrl() }

        return when {
            uri.startsWith("http") -> uri
            uri.startsWith("Qm") -> "$gateway/ipfs/$uri"
            else -> "$gateway/${uri.removeLeadingSlashes()}"
        }.encodeHtmlUrl()
    }

    private fun checkForeignIpfsUri(uri: String, gateway: String, replaceOriginalHost: Boolean): String? {
        val ipfsPathIndex = uri.lastIndexOf(IPFS_PATH_PART)
        if (ipfsPathIndex < 0) {
            return null
        }

        // If URL is valid, and we want to keep original IPFS gateway, return 'as is'
        if (!replaceOriginalHost && isValidUrl(uri)) {
            return uri
        }

        val pathEnd = uri.substring(ipfsPathIndex + IPFS_PATH_PART.length).removeLeadingSlashes()
        // Works only for IPFS CIDs
        if (!isCid(pathEnd.substringBefore("/"))) {
            return null
        }

        return "$gateway/ipfs/$pathEnd"
    }

    private fun checkIpfsAbstractUrl(ipfsUri: String, gateway: String): String? {
        if (ipfsUri.length < IPFS_PREFIX.length) {
            return null
        }

        // Here we're checking links started with 'ipfs:'
        // In some cases there could be prefix in upper/mixed case like 'Ipfs'
        val potentialIpfsPrefix = ipfsUri.substring(0, IPFS_PREFIX.length).lowercase()

        // IPFS prefix not found, abort
        if (potentialIpfsPrefix != IPFS_PREFIX) {
            return null
        }

        val lowerCaseIpfsPrefixUri = IPFS_PREFIX + ipfsUri.substring(IPFS_PREFIX.length).removeLeadingSlashes()

        for (prefix in IPFS_PREFIXES) {
            if (lowerCaseIpfsPrefixUri.startsWith(prefix)) {
                val path = lowerCaseIpfsPrefixUri.substring(prefix.length)
                return "$gateway/ipfs/$path"
            }
        }
        // Should not happen, we already found IPFS prefix
        return null

    }

    private fun String.encodeHtmlUrl(): String {
        return this.replace(" ", "%20")
    }

    private fun String.removeLeadingSlashes(): String {
        var result = this
        while (result.startsWith('/')) {
            result = result.trimStart('/')
        }
        return result
    }

    private fun isValidUrl(uri: String): Boolean {
        return try {
            val url = URL(uri)
            (url.protocol == "http" || url.protocol == "https")
        } catch (e: Exception) {
            false
        }
    }

    fun isCid(test: String): Boolean {
        return CID_PATTERN.matcher(test).matches()
    }

    companion object {

        private val CID_PATTERN = Pattern.compile(
            "Qm[1-9A-HJ-NP-Za-km-z]{44,}|b[A-Za-z2-7]{58,}|B[A-Z2-7]{58,}|z[1-9A-HJ-NP-Za-km-z]{48,}|F[0-9A-F]{50,}|f[0-9a-f]{50,}"
        )

        private const val IPFS_PREFIX = "ipfs:/"
        private const val IPFS_PATH_PART = "/ipfs/"

        private val IPFS_PREFIXES = listOf(
            "ipfs:///ipfs/",
            "ipfs://ipfs/",
            "ipfs:/ipfs/",
            IPFS_PREFIX
        )
    }
}
