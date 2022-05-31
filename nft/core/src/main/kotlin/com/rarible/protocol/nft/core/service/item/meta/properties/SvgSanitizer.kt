package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.core.meta.resource.detector.Base64Utils.base64MimeToBytes
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object SvgSanitizer {

    const val SVG_START = "<svg"
    private const val SVG_ENCODED_START = "%3Csvg"

    private const val BASE_64_SVG_PREFIX = "data:image/svg+xml;base64,"
    private const val UTF8_SVG_PREFIX = "data:image/svg+xml;utf8,"

    // Sanitize data if it is SVG, return 'as is' otherwise
    fun sanitize(itemId: ItemId, svgCandidate: String?): String? {
        // TODO Add here?
        val trimmed = svgCandidate?.trim() ?: return null
        return when {
            trimmed.startsWith(SVG_START) -> trimmed
            trimmed.startsWith(BASE_64_SVG_PREFIX) -> base64Decode(itemId, trimmed)
            trimmed.startsWith(UTF8_SVG_PREFIX) -> removeMimeType(itemId, trimmed)

            // Should be last for case when prefixed with UTF8_SVG_PREFIX
            // Trim doesn't work for encoded SVG
            trimmed.contains(SVG_ENCODED_START) -> {
                val decoded = urlDecode(itemId, trimmed)
                // We expected decoded SVG starts with SVG tag.
                // Otherwise, there could be other content (HTML or query param in URL)
                if (decoded.startsWith(SVG_START)) decoded else svgCandidate
            }
            else -> svgCandidate
        }
    }

    private fun urlDecode(itemId: ItemId, encodedSvg: String): String {
        return try {
            URLDecoder.decode(encodedSvg, StandardCharsets.UTF_8.name()).trim()
        } catch (e: Exception) {
            // return as is to see error message
            logMetaLoading(itemId, "Failed to decode SVG: $encodedSvg, error: ${e.message}", true)
            encodedSvg
        }
    }

    private fun base64Decode(itemId: ItemId, encodedSvg: String): String {
        val svgCandidate = String(base64MimeToBytes(encodedSvg.removePrefix(BASE_64_SVG_PREFIX))).trim()
        return checkUrlEncoded(itemId, svgCandidate)
    }

    private fun removeMimeType(itemId: ItemId, svgCandidate: String): String {
        val svg = svgCandidate.removePrefix(UTF8_SVG_PREFIX).trim()
        return checkUrlEncoded(itemId, svg)
    }


    private fun checkUrlEncoded(itemId: ItemId, encodedSvg: String): String {
        return if (encodedSvg.contains(SVG_ENCODED_START)) {
            urlDecode(itemId, encodedSvg)
        } else {
            encodedSvg
        }
    }
}
