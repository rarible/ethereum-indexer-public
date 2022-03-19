package com.rarible.protocol.nft.core.misc.detector

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SVGDetector(url: String) : ContentDetector(url) {

    private val prefixIndex = url.indexOf(svgTag)

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(SVGDetector::class.java)
        const val svgTag = "<svg"
        const val mimeTypePrefix = "image/svg+xml"
        const val spaceCode = "%20"
    }

    override fun canDecode(): Boolean {
        return prefixIndex >= 0
    }

    override fun getData(): String {
        val content = if (spaceCode in url) {
            logger.warn("Broken svg: %s", url)
            val decodedData = url.replace(spaceCode, " ")
            //TODO Workaround for BRAVO-1872.
            decodedData.substring(decodedData.indexOf(svgTag), decodedData.length)
        } else {
            url
        }
        // Fix for corrupted SVG
        return content.replace(
            // TODO what if 'fill: %23'?
            "fill:%23",
            "fill:#"
        )
    }

    override fun getMimeType(): String {
        return mimeTypePrefix
    }

    override fun getDecodedData(): ByteArray? {
        return getData().toByteArray()
    }
}