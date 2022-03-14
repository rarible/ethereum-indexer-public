package com.rarible.protocol.nft.core.misc.detector

import org.apache.commons.codec.binary.Base64
import org.springframework.stereotype.Component
import java.util.*

/**
 * Parser/Detector for URLs in meta like "https://rarible.mypinata.cloud/data:image/png;base64,iVBORw0KGgoAAAANS..."
 */
class Base64Detector(url: String) : ContentDetector(url) {

    private val prefixIndex = url.indexOf(base64prefix)
    private val markerIndex = if (prefixIndex >= 0) url.indexOf(base64marker, prefixIndex) else -1

    override fun canDecode(): Boolean {
        return prefixIndex >= 0 && markerIndex >= 0
    }

    // Don't want to use regex here, not sure how fast it will work on large strings
    companion object {
        private const val base64prefix = "data:image/"
        private const val mimeTypePrefix = "data:"
        private const val base64marker = ";base64,"
    }

    override fun getData(): String {
        val prefixIndex = url.indexOf(base64marker)
        return url.substring(prefixIndex + base64marker.length).trim()
    }

    override fun getDecodedData(): ByteArray? {
        return Base64.decodeBase64(getData())
    }

    override fun getMimeType(): String {
        return url.substring(url.indexOf(mimeTypePrefix) + mimeTypePrefix.length, markerIndex).trim()
    }
}
