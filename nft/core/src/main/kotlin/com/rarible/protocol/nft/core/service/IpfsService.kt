package com.rarible.protocol.nft.core.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetectProcessor
import com.rarible.core.meta.resource.parser.UrlResourceParsingProcessor
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.IPFS_CAPTURE_SPAN_TYPE
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = IPFS_CAPTURE_SPAN_TYPE)
class IpfsService(                                   // TODO Maybe rename to UrlService
    private val parsingProcessor : UrlResourceParsingProcessor,
    private val urlResolver: UrlResolver,
    private val embeddedContentDetectProcessor: EmbeddedContentDetectProcessor
) {

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInnerHttpUrl(url: String) = resolveInternal(url, false)

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(url: String) = resolveInternal(url, true)

    private fun resolveInternal(url: String, isPublic: Boolean): String {
//        val embeddedContent = embeddedContentDetectProcessor.decode(url)
//        if (embeddedContent != null) return embeddedContent.content.decodeToString() // TODO Move this handling outside

        val resource = parsingProcessor.parse(url) ?: return ""  // TODO Add logging here and maybe throw Exception
        return if (isPublic) {
            urlResolver.resolvePublicLink(resource)
        } else {
            urlResolver.resolveInnerLink(resource)
        }
    }

}
