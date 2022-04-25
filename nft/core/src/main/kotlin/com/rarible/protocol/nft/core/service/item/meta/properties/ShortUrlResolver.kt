package com.rarible.protocol.nft.core.service.item.meta.properties

object ShortUrlResolver {

    fun resolve(url: String): String =
        when {
            url.startsWith("ar://") -> url.replaceFirst("ar://", "https://arweave.net/")
            else -> url
        }
}
