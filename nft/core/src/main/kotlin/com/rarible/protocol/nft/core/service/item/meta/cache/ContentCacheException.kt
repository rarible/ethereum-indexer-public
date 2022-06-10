package com.rarible.protocol.nft.core.service.item.meta.cache

class ContentCacheException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
