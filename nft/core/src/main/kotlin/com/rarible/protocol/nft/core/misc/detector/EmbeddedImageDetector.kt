package com.rarible.protocol.nft.core.misc.detector

class EmbeddedImageDetector {
    companion object{
        fun getDetector(url: String): ContentDetector? {
            // SVG could contain BASE64 embedded images, so we should check it first
            val svgDetector = SVGDetector(url)
            if (svgDetector.canDecode()) {
                return svgDetector
            }

            val base64Detector = Base64Detector(url)
            if (base64Detector.canDecode()) {
                return base64Detector
            }
            return null
        }
    }
}
