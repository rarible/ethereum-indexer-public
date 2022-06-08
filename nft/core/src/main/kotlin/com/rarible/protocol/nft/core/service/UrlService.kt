package com.rarible.protocol.nft.core.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.protocol.nft.core.service.item.meta.IPFS_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = IPFS_CAPTURE_SPAN_TYPE)
class UrlService(
    private val urlParser : UrlParser,
    private val urlResolver: UrlResolver
) {

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInnerHttpUrl(url: String, id: String): String? = resolveInternal(url, false, id)

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(url: String, id: String): String? = resolveInternal(url, true, id)

    private fun resolveInternal(url: String, isPublic: Boolean, id: String): String? {
        val resource = urlParser.parse(url)
        if (resource == null) {
            logMetaLoading(id = id, message = "UrlService: Cannot parse and resolve url: $url", warn = true)
            return ""
        }
        return if (isPublic) {
            urlResolver.resolvePublicUrl(resource)
        } else {
            urlResolver.resolveInternalUrl(resource)
        }
    }

    fun parseUrl(url: String): UrlResource? = urlParser.parse(url)

}
