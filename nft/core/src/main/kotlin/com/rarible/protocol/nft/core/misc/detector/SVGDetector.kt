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
        return if (url.startsWith(svgTag)) {
            url
        } else {
            URLDecoder.decode(url.substring(url.indexOf(svgTag), url.length), "UTF-8")
        }
    }

    override fun getMimeType(): String {
        return mimeTypePrefix
    }

    override fun getDecodedData(): ByteArray? {
        return getData().toByteArray()
    }
}
