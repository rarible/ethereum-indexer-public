package com.rarible.protocol.nft.api.service.detector

import com.rarible.protocol.nft.core.misc.detector.Base64Detector
import com.rarible.protocol.nft.core.misc.detector.ContentDetector
import com.rarible.protocol.nft.core.misc.detector.SVGDetector
import org.springframework.stereotype.Component

@Component
class DetectorService {
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
