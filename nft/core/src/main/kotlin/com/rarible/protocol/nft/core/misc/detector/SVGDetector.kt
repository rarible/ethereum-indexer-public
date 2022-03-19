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
        return if (spaceCode in url) {
            logger.warn("Broken svg: %s", url)
            var decodedData = url
                .replace(spaceCode, " ")
                .replace(
                    "fill:%23",
                    "fill:#"
                ) //TODO Workaround for BRAVO-1872.
            decodedData.substring(decodedData.indexOf(svgTag), decodedData.length)

        } else {
            url
        }
    }

    override fun getMimeType(): String {
        return mimeTypePrefix
    }

    override fun getDecodedData(): ByteArray? {
        return getData().toByteArray()
    }
}
