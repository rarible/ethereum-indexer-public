package com.rarible.protocol.nft.core.misc.detector

import java.net.URLDecoder

class SVGDetector(url: String) : ContentDetector(url) {

    private val prefixIndex = url.indexOf(svgTag)

    companion object {
        private const val svgTag = "<svg"
        private const val mimeTypePrefix = "image/svg+xml"
    }

    override fun canDecode(): Boolean {
        return prefixIndex >= 0
    }

    override fun getData(): String {
        val content = if (url.startsWith(svgTag)) {
            url
        } else {
            URLDecoder.decode(url.substring(prefixIndex, url.length), "UTF-8")
        }
        // Not sure why, but in some SVGs there are # chars encoded as %23
        return content.replace("%23", "#")
    }

    override fun getMimeType(): String {
        return mimeTypePrefix
    }

    override fun getDecodedData(): ByteArray {
        return getData().toByteArray()
    }
}
