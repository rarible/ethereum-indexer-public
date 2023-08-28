package com.rarible.protocol.nft.core.service.token.filter

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word

interface TokeByteCodeFilter {
    fun isValid(code: Binary, hash: Word): Boolean
}
