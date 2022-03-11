package com.rarible.protocol.nft.core.misc.detector

class EmbeddedImageDetector {
    companion object{
        fun getDetector(url: String): ContentDetector? {
            val base64Detector = Base64Detector(url)
            if (base64Detector.canDecode()) {
                return base64Detector
            }

            val svgDetector = SVGDetector(url)
            if (svgDetector.canDecode()) {
                return svgDetector
            }
            return null
        }
    }
}
