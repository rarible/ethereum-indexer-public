package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes

sealed class TokenStandard {
    abstract val interfaceId: Bytes?

    object ERC721 : TokenStandard() {
        override val interfaceId: Bytes
            get() = Binary.apply("0x80ac58cd")
    }

    object ERC721Deprecated : TokenStandard() {
        override val interfaceId: Bytes
            get() = Binary.apply("0x9a20483d")
    }

    object ERC20 : TokenStandard() {
        override val interfaceId: Bytes?
            get() = null
    }
}