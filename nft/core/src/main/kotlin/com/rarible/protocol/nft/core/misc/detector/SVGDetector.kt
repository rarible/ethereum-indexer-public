package com.rarible.protocol.nft.core.misc.detector

class SVGDetector(url: String) : ContentDetector(url) {

    private val prefixIndex = url.indexOf(svgTag)

    companion object {
        private const val svgTag = "<svg"
        private const val mimeTypePrefix = "image/svg+xml"
        private const val spaceCode = "%20"
    }

    override fun canDecode(): Boolean {
        return prefixIndex >= 0
    }

    override fun getData(): String {
        var decodedData = url.replace(spaceCode, " ").replace(
            "fill:%",
            "fill:#"
        ) //TODO Workaround for BRAVO-1872. Use URLDecoder.decode after fix.
        return decodedData.substring(decodedData.indexOf(svgTag), decodedData.length)
    }

    override fun getMimeType(): String {
        return mimeTypePrefix
    }

    override fun getDecodedData(): ByteArray? {
        return getData().toByteArray()
    }
}
