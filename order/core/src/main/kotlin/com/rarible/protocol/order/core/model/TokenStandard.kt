package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes

sealed class TokenStandard {
    abstract val interfaceId: Bytes?

    object ERC721 : TokenStandard() {
        override val interfaceId: Bytes
            get() = Binary.apply("0x80ac58cd")
    }

    object ERC1155 : TokenStandard() {
        override val interfaceId: Bytes
            get() = Binary.apply("0xd9b67a26")
    }
}
