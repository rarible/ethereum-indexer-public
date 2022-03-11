package com.rarible.protocol.nft.core.misc.detector

import org.springframework.stereotype.Component

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
        return url
    }

    override fun getMimeType(): String {
        return mimeTypePrefix
    }

    override fun getInstance(url: String): ContentDetector {
        return SVGDetector(url)
    }
}
