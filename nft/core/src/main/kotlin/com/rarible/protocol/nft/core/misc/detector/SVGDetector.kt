package com.rarible.protocol.nft.core.misc.detector

import org.springframework.stereotype.Component
import java.net.URLDecoder

@Component
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
        return URLDecoder.decode(url.substring(url.indexOf(svgTag), url.length), "UTF-8")
    }

    override fun getMimeType(): String {
        return mimeTypePrefix
    }
}
