package com.rarible.protocol.nft.core.model

import scalether.domain.Address

sealed class TokenFilter {
    abstract val continuation: String?
    abstract val size: Int

    data class All(
        override val continuation: String?,
        override val size: Int
    ) : TokenFilter()

    data class ByOwner(
        val owner: Address,
        override val continuation: String?,
        override val size: Int
    ) : TokenFilter()
}
