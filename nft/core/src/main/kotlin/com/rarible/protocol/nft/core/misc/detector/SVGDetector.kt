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
        return url.replace(spaceCode, " ").replace(
            "fill:%",
            "fill:#"
        ) //TODO Workaround BRAVO-1872. Consider ability to fix data and decode with URLDecoder.decode(urlNoSpace.substring(urlNoSpace.indexOf(svgTag), urlNoSpace.length), "UTF-8")
    }

    override fun getMimeType(): String {
        return mimeTypePrefix
    }

    override fun getDecodedData(): ByteArray? {
        return getData().toByteArray()
    }
}
