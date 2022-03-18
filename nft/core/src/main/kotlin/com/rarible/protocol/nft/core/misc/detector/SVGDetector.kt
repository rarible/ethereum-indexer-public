package com.rarible.protocol.nft.core.misc.detector

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
