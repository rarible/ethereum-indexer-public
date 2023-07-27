package com.rarible.protocol.nft.core.service.item.meta

class MetaException(
    override val message: String,
    val status: Status
) : Exception(message) {
    enum class Status {
        UnparseableLink,
        UnparseableJson,
        Timeout,
        Unknown
    }
}
