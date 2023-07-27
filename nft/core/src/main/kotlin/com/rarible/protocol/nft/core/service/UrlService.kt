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
    private val urlParser: UrlParser,
    private val urlResolver: UrlResolver
) {

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInternalHttpUrl(url: String): String? =
        urlParser.parse(url)
            ?.let { resolveInternalHttpUrl(it) }

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(url: String): String? =
        urlParser.parse(url)
            ?.let { resolvePublicHttpUrl(it) }

    fun parseUrl(url: String, id: String): UrlResource? {
        val resource = urlParser.parse(url)
        if (resource == null) {
            logMetaLoading(id = id, message = "UrlService: Cannot parse and resolve url: $url", warn = true)
        }
        return resource
    }

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInternalHttpUrl(resource: UrlResource): String = urlResolver.resolveInternalUrl(resource)

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(resource: UrlResource): String = urlResolver.resolvePublicUrl(resource)
}
